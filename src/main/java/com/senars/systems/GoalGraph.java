package com.senars.systems;

import com.senars.core.Goal;
import com.senars.db.DatabaseManager;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Manages the graph of long-term goals within the system's memory.
 * This class is responsible for persisting, linking, and querying Goals.
 */
public class GoalGraph {

    private final ConcurrentMap<String, Goal> goals;
    private final ConcurrentMap<String, Set<GoalRelationshipLink>> goalLinks; // Adjacency list: Source -> {Target, Type}

    public GoalGraph(DatabaseManager dbManager) {
        this.goals = dbManager.getPersistentMap("goal_graph_goals");
        this.goalLinks = dbManager.getPersistentMap("goal_graph_links");
    }

    /**
     * Represents the relationship between two goals in the graph.
     */
    public enum GoalRelationship {
        SUB_GOAL_OF,
        BLOCKS
    }

    /**
     * A record to store a directed link in the goal graph.
     */
    private record GoalRelationshipLink(String targetGoalId, GoalRelationship relationship) implements Serializable {}

    /**
     * Adds a new Goal to the graph or updates an existing one.
     * @param goal The Goal to save.
     */
    public void addGoal(Goal goal) {
        goals.put(goal.id(), goal);
    }

    /**
     * Creates a directed link between two goals.
     * @param sourceGoalId The ID of the source goal.
     * @param targetGoalId The ID of the target goal.
     * @param relationship The type of relationship between them.
     */
    public void linkGoals(String sourceGoalId, String targetGoalId, GoalRelationship relationship) {
        goalLinks.computeIfAbsent(sourceGoalId, k -> ConcurrentHashMap.newKeySet())
                 .add(new GoalRelationshipLink(targetGoalId, relationship));
    }

    /**
     * Finds the highest-priority, active goals that are not blocked by other goals.
     * @return A list of the most pressing, actionable goals, sorted by priority.
     */
    public List<Goal> findHighPriorityActiveGoals() {
        // 1. Find all active goals
        List<Goal> activeGoals = goals.values().stream()
                .filter(g -> g.status() == Goal.Status.ACTIVE)
                .toList();

        // 2. Find all goals that are blocked by an active or blocked goal.
        Set<String> blockedGoalIds = goalLinks.entrySet().stream()
                .flatMap(entry -> {
                    String sourceId = entry.getKey();
                    Goal sourceGoal = goals.get(sourceId);
                    // If the source goal is not completed, it might be blocking others
                    if (sourceGoal != null && sourceGoal.status() != Goal.Status.COMPLETED) {
                        return entry.getValue().stream()
                                .filter(link -> link.relationship() == GoalRelationship.BLOCKS)
                                .map(GoalRelationshipLink::targetGoalId);
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. Filter out the blocked goals and sort by priority
        return activeGoals.stream()
                .filter(goal -> !blockedGoalIds.contains(goal.id()))
                .sorted(Comparator.comparingDouble(Goal::priority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a goal by its unique identifier.
     * @param goalId The ID of the goal to retrieve.
     * @return An Optional containing the Goal if found, otherwise empty.
     */
    public Optional<Goal> getGoalById(String goalId) {
        return Optional.ofNullable(goals.get(goalId));
    }
}
