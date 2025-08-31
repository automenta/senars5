package com.senars.cycle;

import com.senars.core.*;
import com.senars.systems.IGovernanceLayer;
import com.senars.systems.IMemoryNexus;
import com.senars.systems.immemory.InMemoryMemoryNexus;
import com.senars.systems.immemory.PassThroughGovernanceLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitiveCycleTest {

    private CognitiveCycle cognitiveCycle;

    @Mock
    private IPerceptionSystem perceptionSystem;
    @Mock
    private ICognitiveProcessor cognitiveProcessor;
    @Mock
    private IActionSystem actionSystem;

    @Spy
    private IMemoryNexus memoryNexus = new InMemoryMemoryNexus();
    @Spy
    private IGovernanceLayer governanceLayer = new PassThroughGovernanceLayer();

    // Using a more robust spyable implementation of the funnel for testing
    @Spy
    private IAttentionFunnel attentionFunnel = new AttentionFunnelForTest();

    static class AttentionFunnelForTest implements IAttentionFunnel {
        private final AtomicReference<Thought> candidate = new AtomicReference<>();
        @Override
        public void addCandidate(Thought thought) { candidate.set(thought); }
        @Override
        public Optional<Thought> selectFocusThought() { return Optional.ofNullable(candidate.getAndSet(null)); }
    }

    @BeforeEach
    void setUp() {
        cognitiveCycle = new CognitiveCycle(
            perceptionSystem,
            attentionFunnel,
            cognitiveProcessor,
            actionSystem,
            memoryNexus,
            governanceLayer
        );
    }

    private Thought createTestThought(ThoughtType type) {
        return new Thought(
            UUID.randomUUID().toString(),
            new ThoughtContent("test content for " + type, null, null, null, null),
            new ThoughtState(1.0, 1.0, 1.0),
            new ThoughtMetadata(type, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void step_processesFocusThoughtAndAddsToMemory() {
        Thought focusThought = createTestThought(ThoughtType.BELIEF);
        Thought newThought = createTestThought(ThoughtType.REPORT);

        attentionFunnel.addCandidate(focusThought); // Prime the funnel
        when(cognitiveProcessor.process(focusThought)).thenReturn(List.of(newThought));

        cognitiveCycle.step();

        verify(cognitiveProcessor).process(focusThought);
        verify(memoryNexus).saveThought(newThought);
        verify(attentionFunnel).addCandidate(newThought);
        verify(actionSystem, never()).executePlan(any());
    }

    @Test
    void step_handlesActionPlanApprovalAndExecution() {
        Thought focusThought = createTestThought(ThoughtType.GOAL);
        Thought actionPlan = createTestThought(ThoughtType.ACTION_PLAN);

        attentionFunnel.addCandidate(focusThought); // Prime the funnel
        when(cognitiveProcessor.process(focusThought)).thenReturn(List.of(actionPlan));

        cognitiveCycle.step();

        verify(governanceLayer).reviewPlan(actionPlan);
        verify(actionSystem).executePlan(actionPlan);
        // Action plans should not be added back to the funnel for consideration
        verify(attentionFunnel, never()).addCandidate(actionPlan);
    }

    @Test
    void step_handlesActionPlanVetoAndCreatesReplanGoal() {
        Thought focusThought = createTestThought(ThoughtType.GOAL);
        Thought actionPlan = createTestThought(ThoughtType.ACTION_PLAN);
        String vetoReason = "This is unsafe!";

        attentionFunnel.addCandidate(focusThought); // Prime the funnel
        when(cognitiveProcessor.process(focusThought)).thenReturn(List.of(actionPlan));
        // Override the spy's default pass-through behavior for this test
        when(governanceLayer.reviewPlan(actionPlan)).thenReturn(Optional.of(vetoReason));

        cognitiveCycle.step();

        verify(governanceLayer).reviewPlan(actionPlan);
        verify(actionSystem, never()).executePlan(actionPlan);
        // Verify a new GOAL was created and added to the funnel
        verify(memoryNexus, times(2)).saveThought(any(Thought.class)); // original plan + replan goal
        verify(attentionFunnel).addCandidate(argThat(t ->
            t.metadata().type() == ThoughtType.GOAL && t.content().text().contains(vetoReason)
        ));
    }

    @Test
    void step_perceivesNewThoughtsAndAddsToFunnel() {
        Thought perceivedThought = createTestThought(ThoughtType.BELIEF);
        perceivedThought = new Thought(
            perceivedThought.id(),
            new ThoughtContent("A new perception", null, null, null, null),
            perceivedThought.state(),
            perceivedThought.metadata()
        );

        // When the perception system runs, it returns a new thought.
        when(perceptionSystem.perceive()).thenReturn(List.of(perceivedThought));
        // There are no other thoughts in the system to start.
        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.empty());

        cognitiveCycle.step();

        // Verify the perception system was checked.
        verify(perceptionSystem).perceive();
        // Verify the new thought was added to the attention funnel for future consideration.
        verify(attentionFunnel).addCandidate(perceivedThought);
        // Verify that since there was no focus thought, the processor did not run.
        verify(cognitiveProcessor, never()).process(any());
    }
}
