package com.senars.logic;

import com.senars.core.*;
import com.senars.systems.GoalGraph;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A planner that reasons about the GoalGraph to generate proactive, goal-oriented tasks.
 * This service allows the agent to be self-directed instead of purely reactive.
 */
public class GoalOrientedPlanner {

    private final GoalGraph goalGraph;
    private final UnifiedCausalReasoner ucr;

    public GoalOrientedPlanner(GoalGraph goalGraph, UnifiedCausalReasoner ucr) {
        this.goalGraph = goalGraph;
        this.ucr = ucr;
    }

    /**
     * Examines the current goals and generates the next concrete task to make progress.
     *
     * @return An Optional containing a new TASK Thought if a relevant task can be generated, otherwise empty.
     */
    public Optional<Thought> generateNextTask() {
        List<Goal> activeGoals = goalGraph.findHighPriorityActiveGoals();

        if (activeGoals.isEmpty()) {
            return Optional.empty(); // No active goals to pursue.
        }

        // Select the highest priority goal to focus on.
        Goal currentGoal = activeGoals.getFirst();

        // 1. Create a "focus" Thought from the current Goal.
        // This Thought acts as the starting point for the UCR's forward reasoning.
        Thought goalAsThought = new Thought(
                "goal_focus_" + currentGoal.id(),
                new ThoughtContent(
                        "My current objective is: " + currentGoal.description(),
                        null, null, null, null, null, null
                ),
                new ThoughtState(1.0, 100.0, 1.0), // High salience to guide reasoning
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.SYSTEM_PLANNER,
                        List.of(),
                        Instant.now()
                )
        );

        // 2. Use the UCR to reason forward from this goal-thought to generate actionable tasks.
        // The UCR's job is to break the high-level goal down into a concrete next step.
        List<Thought> generatedThoughts = ucr.reason(goalAsThought, "forward", UnifiedCausalReasoner.ReasoningOptions.defaults());

        // 3. Find the first generated thought that is an actionable TASK.
        return generatedThoughts.stream()
                .filter(t -> t.metadata().type() == ThoughtType.ACTION)
                .findFirst();
    }
}
