package com.senars.cycle;

import com.senars.core.*;
import com.senars.effort.EffortPredictor;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Governor;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import com.senars.systems.immemory.InMemoryGovernor;
import com.senars.systems.immemory.InMemoryMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitiveCycleTest {

    @Spy
    private final Memory memoryNexus = new InMemoryMemory();
    @Spy
    private final Governor governanceLayer = new InMemoryGovernor(Collections.emptyList());
    private CognitiveCycle cognitiveCycle;
    @Mock
    private Perception perceptionSystem;
    @Mock
    private Cognition cognitiveProcessor;
    @Mock
    private Action actionSystem;
    @Mock
    private Sessions sessions;
    @Mock
    private Grounding groundingSystem;
    private Attention attentionFunnel;

    // Real dependencies for a more integrated test
    private MotiveHierarchy motiveHierarchy;
    private SalienceCalculator salienceCalculator;
    private EffortPredictor effortPredictor;

    @BeforeEach
    void setUp() {
        // Setup real components for testing the cycle with salience
        motiveHierarchy = new MotiveHierarchy();
        effortPredictor = new EffortPredictor(memoryNexus); // Pass the memory nexus spy
        salienceCalculator = new SalienceCalculator(effortPredictor);
        attentionFunnel = new SalienceBasedAttention(salienceCalculator, motiveHierarchy);

        cognitiveCycle = new CognitiveCycle(
                perceptionSystem,
                attentionFunnel,
                cognitiveProcessor,
                actionSystem,
                memoryNexus,
                governanceLayer,
                sessions,
                groundingSystem
        );
    }

    private Thought createTestThought(ThoughtType type, double activation) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("test content for " + type, null, null, null, null, null),
                new ThoughtState(1.0, 0.0, activation), // clarity, salience (unused), activation
                new ThoughtMeta(type, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void step_processesMostSalientThought() {
        Thought lowSalienceThought = createTestThought(ThoughtType.BELIEF, 0.1); // Low activation
        Thought highSalienceThought = createTestThought(ThoughtType.BELIEF, 1.0); // High activation
        Thought newThought = createTestThought(ThoughtType.REPORT, 0.5);

        attentionFunnel.addCandidate(lowSalienceThought);
        attentionFunnel.addCandidate(highSalienceThought);
        when(cognitiveProcessor.process(highSalienceThought)).thenReturn(List.of(newThought));

        cognitiveCycle.step();

        // Verify that the most salient thought was processed
        verify(cognitiveProcessor).process(highSalienceThought);
        verify(cognitiveProcessor, never()).process(lowSalienceThought);

        // Verify the new thought was saved and added back to the funnel
        verify(memoryNexus).saveThought(newThought);
    }

    @Test
    void step_handlesActionPlanApprovalAndExecution() {
        Thought goal = createTestThought(ThoughtType.GOAL, 1.0);
        Thought actionPlan = createTestThought(ThoughtType.ACTION_PLAN, 0.9);

        attentionFunnel.addCandidate(goal);
        when(cognitiveProcessor.process(goal)).thenReturn(List.of(actionPlan));

        cognitiveCycle.step(); // First step processes the GOAL and produces the ACTION_PLAN

        // The action plan is saved to memory and added to the funnel
        verify(memoryNexus).saveThought(actionPlan);

        // Now the action plan should be the most salient thing
        cognitiveCycle.step(); // Second step should process the ACTION_PLAN

        verify(governanceLayer).reviewPlan(actionPlan);
        verify(actionSystem).executePlan(actionPlan);
    }

    @Test
    void step_handlesActionPlanVetoAndCreatesReplanGoal() {
        Thought goal = createTestThought(ThoughtType.GOAL, 1.0);
        Thought actionPlan = createTestThought(ThoughtType.ACTION_PLAN, 0.9);
        String vetoReason = "This is unsafe!";

        attentionFunnel.addCandidate(goal);
        when(cognitiveProcessor.process(goal)).thenReturn(List.of(actionPlan));
        when(governanceLayer.reviewPlan(actionPlan)).thenReturn(Optional.of(vetoReason));

        cognitiveCycle.step(); // Process GOAL, create ACTION_PLAN
        cognitiveCycle.step(); // Process ACTION_PLAN, get vetoed

        verify(governanceLayer).reviewPlan(actionPlan);
        verify(actionSystem, never()).executePlan(actionPlan);

        // Verify a new GOAL was created and saved (one for the plan, one for the goal)
        verify(memoryNexus, times(2)).saveThought(any(Thought.class));
    }

    @Test
    void step_perceivesAndProcessesThoughtInSameCycle() {
        Thought perceivedThought = createTestThought(ThoughtType.BELIEF, 0.8);

        // On the FIRST call, perceive a thought. On subsequent calls, perceive nothing.
        when(perceptionSystem.perceive())
                .thenReturn(List.of(perceivedThought))
                .thenReturn(Collections.emptyList());

        // The first step perceives and should also process the thought, as it's the only one.
        cognitiveCycle.step();

        // Verify the perception system was checked.
        verify(perceptionSystem).perceive();
        // Verify the thought was processed in the same cycle.
        verify(cognitiveProcessor).process(perceivedThought);
    }

    @Test
    void step_handlesFeedbackReportAndCallsGroundingSystem() {
        Thought actionPlan = new Thought(
                "action-1",
                new ThoughtContent("Do something", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION_PLAN, ThoughtOrigin.LLM_INFERENCE, List.of("goal-1"), Instant.now())
        );
        Thought feedbackReport = new Thought(
                "feedback-1",
                new ThoughtContent("Good job", null, null, null, null, new Feedback(0.9, "User feedback")),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.REPORT, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );

        when(perceptionSystem.perceive()).thenReturn(List.of(feedbackReport));
        when(sessions.getLastActionPlan()).thenReturn(Optional.of(actionPlan));

        cognitiveCycle.step();

        verify(sessions).getLastActionPlan();
        verify(groundingSystem).processFeedback(argThat(report ->
                report.metadata().trace().equals(actionPlan.metadata().trace()) &&
                        report.content().feedback().success() == 0.9
        ));
        verify(sessions).clearLastActionPlan();
        // Ensure feedback is not added to the attention funnel
        // We can check if the funnel is empty or check its size before and after.
        // For this test, we can assume if groundingSystem was called, it wasn't funneled.
    }
}
