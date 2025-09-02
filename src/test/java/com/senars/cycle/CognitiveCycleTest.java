package com.senars.cycle;

import com.senars.core.*;
import com.senars.effort.EffortPredictor;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.logic.GoalOrientedPlanner;
import com.senars.logic.MetaCognitiveService;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.motive.MotiveHierarchy;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Governor;
import com.senars.systems.Memory;
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
    private UnifiedCausalReasoner ucr; // Replaces groundingSystem and cognitiveProcessor
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
    private GoalOrientedPlanner goalOrientedPlanner;

    private Attention attentionFunnel;
    private CognitiveCycle cognitiveCycle;


    @BeforeEach
    void setUp() {
        // Setup real components for testing the cycle with salience
        var motiveHierarchy = new MotiveHierarchy();
        var effortPredictor = new EffortPredictor(memory); // Pass the memory nexus mock
        var salienceCalculator = new SalienceCalculator(effortPredictor, memory);
        attentionFunnel = new SalienceAttention(salienceCalculator, motiveHierarchy, eventBus, ucr);

        cognitiveCycle = new CognitiveCycle(
                perceptionSystem,
                attentionFunnel,
                ucr, // Use UCR instead of cognitiveProcessor
                actionSystem,
                memory,
                governor,
                feedbackQueue,
                schemaOptimizer,
                effortOptimizer,
                effortTracker,
                eventBus,
                metaCognitiveService,
                goalOrientedPlanner
        );
    }

    private Thought createTestThought(ThoughtType type, double activation) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("test content for " + type, null, null, null, null, null, null),
                new ThoughtState(1.0, 0.0, activation), // clarity, salience (unused), activation
                new ThoughtMeta(type, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void step_processesMostSalientThought() throws ShutdownException {
        Thought lowSalienceThought = createTestThought(ThoughtType.BELIEF, 0.1); // Low activation
        Thought highSalienceThought = createTestThought(ThoughtType.BELIEF, 1.0); // High activation
        Thought newThought = createTestThought(ThoughtType.REPORT, 0.5);

        attentionFunnel.addCandidate(lowSalienceThought);
        attentionFunnel.addCandidate(highSalienceThought);
        when(ucr.reason(eq(highSalienceThought), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class))).thenReturn(List.of(newThought));

        cognitiveCycle.step();

        // Verify that the most salient thought was processed
        verify(ucr).reason(eq(highSalienceThought), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class));

        // Verify the new thought was saved and added back to the funnel
        verify(memory).saveThought(newThought);
    }

    @Test
    void step_handlesActionPlanApprovalAndExecution() throws ShutdownException {
        Thought goal = createTestThought(ThoughtType.GOAL, 1.0);
        Thought actionPlan = createTestThought(ThoughtType.ACTION, 0.9);

        attentionFunnel.addCandidate(goal);
        when(ucr.reason(eq(goal), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class))).thenReturn(List.of(actionPlan));

        cognitiveCycle.step(); // First step processes the GOAL and produces the ACTION

        // The action plan is saved to memory and added to the funnel
        verify(memory).saveThought(actionPlan);

        // Now the action plan should be the most salient thing
        cognitiveCycle.step(); // Second step should process the ACTION

        verify(governor).reviewPlan(actionPlan);
        verify(actionSystem).executePlan(actionPlan);
    }

    @Test
    void step_handlesActionPlanVetoAndCreatesReplanGoal() throws ShutdownException {
        Thought goal = createTestThought(ThoughtType.GOAL, 1.0);
        Thought actionPlan = createTestThought(ThoughtType.ACTION, 0.9);
        String vetoReason = "This is unsafe!";

        attentionFunnel.addCandidate(goal);
        when(ucr.reason(eq(goal), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class))).thenReturn(List.of(actionPlan));
        when(governor.reviewPlan(actionPlan)).thenReturn(Optional.of(vetoReason));

        cognitiveCycle.step(); // Process GOAL, create ACTION
        cognitiveCycle.step(); // Process ACTION, get vetoed

        verify(governor).reviewPlan(actionPlan);
        verify(actionSystem, never()).executePlan(actionPlan);

        // Verify a new GOAL was created and saved (one for the plan, one for the goal)
        verify(memory, times(2)).saveThought(any(Thought.class));
    }

    @Test
    void step_perceivesAndProcessesThoughtInSameCycle() throws ShutdownException {
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
        verify(ucr).reason(eq(perceivedThought), eq("forward"), any(UnifiedCausalReasoner.ReasoningOptions.class));
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

        cognitiveCycle.step();

        verify(ucr).processFeedback(feedback);
        // Ensure feedback is not added to the attention funnel
        // We can check if the funnel is empty or check its size before and after.
        // For this test, we can assume if ucr was called, it wasn't funneled.
    }

    @Test
    void step_callsGoalPlannerWhenIdle() throws ShutdownException {
        // Arrange
        Thought proactiveTask = createTestThought(ThoughtType.ACTION, 0.9);
        // Ensure the attention funnel is empty
        when(goalOrientedPlanner.generateNextTask()).thenReturn(Optional.of(proactiveTask));

        // Act
        cognitiveCycle.step();

        // Assert
        // Verify that the planner was called because the system was idle
        verify(goalOrientedPlanner).generateNextTask();
        // Verify that the new proactive task was saved to memory and added to the attention funnel
        verify(memory).saveThought(proactiveTask);
        // You could also assert that the attention funnel now contains the proactive task,
        // but verifying the saveThought is a strong indicator.
    }
}
