package com.senars.events;

import com.senars.core.Thought;

/**
 * A container class for all specific event types in the system.
 * Using records for immutable, concise event data carriers.
 */
public final class Events {

    // Private constructor to prevent instantiation
    private Events() {}

    public record NewThoughtCreatedEvent(Thought thought) implements Event {}
    public record FocusThoughtSelectedEvent(Thought thought) implements Event {}
    public record ActionPlanApprovedEvent(Thought actionPlan) implements Event {}
    public record ActionPlanVetoedEvent(Thought actionPlan, String reason) implements Event {}
    public record ClarityUpdatedEvent(String thoughtId, double oldClarity, double newClarity) implements Event {}
    public record SchemaOptimizationGoalCreatedEvent(Thought goal) implements Event {}
    public record GoalAchievedEvent(Thought goal) implements Event {}
    public record GoalFailedEvent(Thought goal) implements Event {}

}
