package com.senars.systems;

import com.senars.core.Goal;
import com.senars.db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalGraphTest {

    @Mock
    private DatabaseManager dbManager;

    private GoalGraph goalGraph;

    @BeforeEach
    void setUp() {
        // Mock the database manager to return in-memory maps for testing
        when(dbManager.getPersistentMap("goal_graph_goals")).thenReturn(new ConcurrentHashMap<>());
        when(dbManager.getPersistentMap("goal_graph_links")).thenReturn(new ConcurrentHashMap<>());
        goalGraph = new GoalGraph(dbManager);
    }

    @Test
    void testAddAndGetGoal() {
        Goal goal = new Goal("g1", "Test Goal 1", Goal.Status.ACTIVE, 100.0);
        goalGraph.addGoal(goal);

        Optional<Goal> retrieved = goalGraph.getGoalById("g1");
        assertTrue(retrieved.isPresent());
        assertEquals("Test Goal 1", retrieved.get().description());
    }

    @Test
    void testFindHighPriorityActiveGoals_Simple() {
        Goal g1 = new Goal("g1", "Low Prio", Goal.Status.ACTIVE, 50.0);
        Goal g2 = new Goal("g2", "High Prio", Goal.Status.ACTIVE, 100.0);
        Goal g3 = new Goal("g3", "Completed Goal", Goal.Status.COMPLETED, 200.0);

        goalGraph.addGoal(g1);
        goalGraph.addGoal(g2);
        goalGraph.addGoal(g3);

        List<Goal> result = goalGraph.findHighPriorityActiveGoals();

        assertEquals(2, result.size());
        assertEquals("g2", result.get(0).id()); // g2 should be first due to higher priority
        assertEquals("g1", result.get(1).id());
    }

    @Test
    void testFindHighPriorityActiveGoals_WithBlocking() {
        Goal g1 = new Goal("g1", "Blocker Goal", Goal.Status.ACTIVE, 100.0);
        Goal g2 = new Goal("g2", "Blocked Goal", Goal.Status.ACTIVE, 200.0);

        goalGraph.addGoal(g1);
        goalGraph.addGoal(g2);
        goalGraph.linkGoals(g1.id(), g2.id(), GoalGraph.GoalRelationship.BLOCKS);

        List<Goal> result = goalGraph.findHighPriorityActiveGoals();

        assertEquals(1, result.size());
        assertEquals("g1", result.getFirst().id()); // Only the blocker goal should be returned
    }

    @Test
    void testFindHighPriorityActiveGoals_WithCompletedBlocker() {
        Goal g1 = new Goal("g1", "Completed Blocker", Goal.Status.COMPLETED, 100.0);
        Goal g2 = new Goal("g2", "Unblocked Goal", Goal.Status.ACTIVE, 200.0);

        goalGraph.addGoal(g1);
        goalGraph.addGoal(g2);
        goalGraph.linkGoals(g1.id(), g2.id(), GoalGraph.GoalRelationship.BLOCKS);

        List<Goal> result = goalGraph.findHighPriorityActiveGoals();

        assertEquals(1, result.size());
        assertEquals("g2", result.getFirst().id()); // The previously blocked goal should now be active
    }
}
