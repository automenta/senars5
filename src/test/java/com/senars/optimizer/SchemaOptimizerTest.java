package com.senars.optimizer;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaOptimizerTest {

    @Mock
    private Memory memory;
    @Mock
    private EventBus eventBus;

    private SchemaOptimizer schemaOptimizer;

    @BeforeEach
    void setUp() {
        schemaOptimizer = new SchemaOptimizer(memory, eventBus);
    }

    private Thought createSchema(String id) {
        return new Thought(id,
                new ThoughtContent("Test Schema", "senars:test_schema", null, null, "Do something.", null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now()));
    }

    private Thought createBelief(String id, String schemaId) {
        return new Thought(id,
                new ThoughtContent("Belief based on schema", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, List.of(schemaId), Instant.now()));
    }

    @Test
    void run_withSlowSchema_generatesOptimizationGoal() {
        // Arrange
        Thought schema = createSchema("schema1");
        when(memory.getThoughtById("schema1")).thenReturn(Optional.of(schema));

        // Simulate 15 slow but successful executions
        for (int i = 0; i < 15; i++) {
            Thought belief = createBelief("belief" + i, "schema1");
            Feedback feedback = new Feedback(ActionStatus.SUCCESS, "test.tool", "test", 2000L, belief);
            schemaOptimizer.onActionExecuted(new Events.ActionExecutedEvent(feedback));
        }

        // Act
        List<Thought> goals = schemaOptimizer.run();

        // Assert
        assertEquals(1, goals.size(), "Should generate one optimization goal for slow schema.");
        verify(eventBus).publish(any(Events.SchemaOptimizationGoalCreatedEvent.class));
    }

    @Test
    void run_withUnsuccessfulSchema_generatesOptimizationGoal() {
        // Arrange
        Thought schema = createSchema("schema2");
        when(memory.getThoughtById("schema2")).thenReturn(Optional.of(schema));

        // Simulate 15 executions, most of which fail
        for (int i = 0; i < 12; i++) {
            Thought belief = createBelief("belief" + i, "schema2");
            Feedback feedback = new Feedback(ActionStatus.FAILURE, "test.tool", "test", 100L, belief);
            schemaOptimizer.onActionExecuted(new Events.ActionExecutedEvent(feedback));
        }
        for (int i = 12; i < 15; i++) {
            Thought belief = createBelief("belief" + i, "schema2");
            Feedback feedback = new Feedback(ActionStatus.SUCCESS, "test.tool", "test", 100L, belief);
            schemaOptimizer.onActionExecuted(new Events.ActionExecutedEvent(feedback));
        }

        // Act
        List<Thought> goals = schemaOptimizer.run();

        // Assert
        assertEquals(1, goals.size(), "Should generate one optimization goal for unsuccessful schema.");
        verify(eventBus).publish(any(Events.SchemaOptimizationGoalCreatedEvent.class));
    }

    @Test
    void run_withWellPerformingSchema_doesNotGenerateGoal() {
        // Arrange
        Thought schema = createSchema("schema3");
        when(memory.getThoughtById("schema3")).thenReturn(Optional.of(schema));

        // Simulate 15 fast and successful executions
        for (int i = 0; i < 15; i++) {
            Thought belief = createBelief("belief" + i, "schema3");
            Feedback feedback = new Feedback(ActionStatus.SUCCESS, "test.tool", "test", 100L, belief);
            schemaOptimizer.onActionExecuted(new Events.ActionExecutedEvent(feedback));
        }

        // Act
        List<Thought> goals = schemaOptimizer.run();

        // Assert
        assertTrue(goals.isEmpty(), "Should not generate a goal for a well-performing schema.");
        verify(eventBus, never()).publish(any());
    }

    @Test
    void run_withSchemaUsedTooFewTimes_doesNotGenerateGoal() {
        // Arrange
        Thought schema = createSchema("schema4");
        when(memory.getThoughtById("schema4")).thenReturn(Optional.of(schema));

        // Only 5 uses, which is below the threshold of 10
        for (int i = 0; i < 5; i++) {
            Thought belief = createBelief("belief" + i, "schema4");
            Feedback feedback = new Feedback(ActionStatus.FAILURE, "test.tool", "test", 3000L, belief);
            schemaOptimizer.onActionExecuted(new Events.ActionExecutedEvent(feedback));
        }

        // Act
        List<Thought> goals = schemaOptimizer.run();

        // Assert
        assertTrue(goals.isEmpty(), "Should not generate a goal for a schema used too few times.");
        verify(eventBus, never()).publish(any());
    }
}
