package com.senars.cycle;

import com.senars.attention.Attention;
import com.senars.core.*;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.logic.GoalOrientedPlanner;
import com.senars.logic.MetaCognitiveService;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.logic.mdr.MDRService;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.systems.Governor;
import com.senars.systems.Memory;
import com.senars.ui.ConsolePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitiveCycleTest {

    @Mock
    private Memory memory;
    @Mock
    private Governor governor;
    @Mock
    private Perception perceptionSystem;
    @Mock
    private Action actionSystem;
    @Mock
    private UnifiedCausalReasoner ucr;
    @Mock
    private EventBus eventBus;
    @Mock
    private SchemaOptimizer schemaOptimizer;
    @Mock
    private EffortModelOptimizer effortOptimizer;
    @Mock
    private EffortTracker effortTracker;
    @Mock
    private ActionFeedbackQueue feedbackQueue;
    @Mock
    private MetaCognitiveService metaCognitiveService;
    @Mock
    private MDRService mdrService;
    @Mock
    private GoalOrientedPlanner goalOrientedPlanner;
    @Mock
    private ConsolePrinter consolePrinter;
    @Mock
    private Attention attentionFunnel;

    private CognitiveCycle cognitiveCycle;


    @BeforeEach
    void setUp() {
        CognitiveCycleServices services = new CognitiveCycleServices(
                perceptionSystem,
                attentionFunnel,
                ucr,
                actionSystem,
                memory,
                governor,
                feedbackQueue,
                schemaOptimizer,
                effortOptimizer,
                effortTracker,
                eventBus,
                metaCognitiveService,
                mdrService,
                goalOrientedPlanner,
                consolePrinter
        );
        cognitiveCycle = new CognitiveCycle(services);
        lenient().when(ucr.reason(any(), any(), any())).thenReturn(List.of());
    }

    private Thought createTestThought(ThoughtType type, double activation) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("test content for " + type, null, null, null, null, null, null),
                new ThoughtState(1.0, 0.0, activation),
                new ThoughtMeta(type, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void step_processesMostSalientThought() throws ShutdownException {
        Thought highSalienceThought = createTestThought(ThoughtType.BELIEF, 1.0);
        Thought newThought = createTestThought(ThoughtType.REPORT, 0.5);

        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.of(highSalienceThought));
        when(ucr.reason(eq(highSalienceThought), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class))).thenReturn(List.of(newThought));

        cognitiveCycle.step();

        verify(ucr).reason(eq(highSalienceThought), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class));
        verify(memory).saveThought(newThought);
        verify(attentionFunnel).addCandidate(newThought);
    }

    @Test
    void step_handlesActionPlanApprovalAndExecution() throws ShutdownException {
        Thought goal = createTestThought(ThoughtType.GOAL, 1.0);
        Thought actionPlan = createTestThought(ThoughtType.ACTION, 0.9);

        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.of(goal), Optional.of(actionPlan));
        when(ucr.reason(eq(goal), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class))).thenReturn(List.of(actionPlan));

        cognitiveCycle.step();
        cognitiveCycle.step();

        verify(governor).reviewPlan(actionPlan);
        verify(actionSystem).executePlan(actionPlan);
    }

    @Test
    void step_handlesActionPlanVetoAndCreatesReplanGoal() throws ShutdownException {
        Thought goal = createTestThought(ThoughtType.GOAL, 1.0);
        Thought actionPlan = createTestThought(ThoughtType.ACTION, 0.9);
        String vetoReason = "This is unsafe!";

        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.of(goal), Optional.of(actionPlan));
        when(ucr.reason(eq(goal), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class))).thenReturn(List.of(actionPlan));
        when(governor.reviewPlan(actionPlan)).thenReturn(Optional.of(vetoReason));

        cognitiveCycle.step();
        cognitiveCycle.step();

        verify(governor).reviewPlan(actionPlan);
        verify(actionSystem, never()).executePlan(actionPlan);
        verify(memory, times(2)).saveThought(any(Thought.class));
    }

    @Test
    void step_perceivesAndProcessesThoughtInSameCycle() throws ShutdownException {
        Thought perceivedThought = createTestThought(ThoughtType.BELIEF, 0.8);

        when(perceptionSystem.perceive()).thenReturn(List.of(perceivedThought));
        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.empty());

        cognitiveCycle.step();

        verify(perceptionSystem).perceive();
        verify(attentionFunnel).addCandidate(perceivedThought);
    }

    @Test
    void step_handlesFeedbackReportAndCallsUCR() throws ShutdownException {
        Thought actionPlan = new Thought(
                "action-1",
                new ThoughtContent("Do something", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.LLM_INFERENCE, List.of("goal-1"), Instant.now())
        );
        Feedback feedback = new Feedback(ActionStatus.SUCCESS, "test.tool", "Good job", 100L, actionPlan);

        when(feedbackQueue.poll()).thenReturn(feedback);
        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.empty());

        cognitiveCycle.step();

        verify(ucr).processFeedback(feedback);
    }

    @Test
    void step_callsGoalPlannerWhenIdle() throws ShutdownException {
        Thought proactiveTask = createTestThought(ThoughtType.ACTION, 0.9);
        when(attentionFunnel.selectFocusThought()).thenReturn(Optional.empty());
        when(goalOrientedPlanner.generateNextTask()).thenReturn(Optional.of(proactiveTask));

        cognitiveCycle.step();

        verify(goalOrientedPlanner).generateNextTask();
        verify(memory).saveThought(proactiveTask);
    }
}
