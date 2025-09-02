package com.senars.optimizer;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.systems.Memory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
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

    @InjectMocks
    private SchemaOptimizer schemaOptimizer;

    private Thought createSchema(String id) {
        return new Thought(id,
                new ThoughtContent("Test Schema", "senars:test_schema", null, null, "Do something.", null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now()));
    }

    private Thought createBelief(String id, String schemaId, double clarity) {
        return new Thought(id,
                new ThoughtContent("Belief based on schema", null, null, null, null, null, null),
                new ThoughtState(clarity, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, List.of(schemaId), Instant.now()));
    }

    @Test
    void run_withUnderperformingSchema_generatesOptimizationGoal() {
        // Arrange
        Thought schema = createSchema("schema1");
        List<Thought> allThoughts = new ArrayList<>();
        allThoughts.add(schema);

        // Add 15 beliefs, 12 of which are low clarity, bringing avg clarity below 0.5
        // 12 * 0.2 = 2.4; 3 * 1.0 = 3.0; Total clarity = 5.4; Avg = 5.4 / 15 = 0.36
        for (int i = 0; i < 12; i++) {
            allThoughts.add(createBelief("belief" + i, "schema1", 0.2));
        }
        for (int i = 12; i < 15; i++) {
            allThoughts.add(createBelief("belief" + i, "schema1", 1.0));
        }

        // Act
        for (Thought thought : allThoughts) {
            if (thought.metadata().type() == ThoughtType.BELIEF) {
                Feedback feedback = new Feedback(thought.state().clarity() > 0.5 ? ActionStatus.SUCCESS : ActionStatus.FAILURE, "test.tool", "test", 2500L, thought);
                schemaOptimizer.onActionExecuted(new Events.ActionExecutedEvent(feedback));
            }
        }
        List<Thought> goals = schemaOptimizer.run();

        // Assert
        assertEquals(1, goals.size(), "Should generate one optimization goal.");
        Thought goal = goals.getFirst();
        assertEquals(ThoughtType.GOAL, goal.metadata().type());
        assertEquals(SchemaOptimizer.REWRITE_SCHEMA_SYMBOLIC, goal.content().symbolic());
        assertTrue(goal.metadata().trace().contains("schema1"));

        // Verify event was published
        ArgumentCaptor<Events.SchemaOptimizationGoalCreatedEvent> eventCaptor = ArgumentCaptor.forClass(Events.SchemaOptimizationGoalCreatedEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        assertEquals(goal, eventCaptor.getValue().goal());
    }

    @Test
    void run_withWellPerformingSchema_doesNotGenerateGoal() {
        // Arrange
        Thought schema = createSchema("schema1");
        List<Thought> allThoughts = new ArrayList<>();
        allThoughts.add(schema);

        // Average clarity is high
        for (int i = 0; i < 15; i++) {
            allThoughts.add(createBelief("belief" + i, "schema1", 0.9));
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
        Thought schema = createSchema("schema1");
        List<Thought> allThoughts = new ArrayList<>();
        allThoughts.add(schema);

        // Only 5 uses, which is below the threshold of 10
        for (int i = 0; i < 5; i++) {
            allThoughts.add(createBelief("belief" + i, "schema1", 0.1));
        }

        // Act
        List<Thought> goals = schemaOptimizer.run();

        // Assert
        assertTrue(goals.isEmpty(), "Should not generate a goal for a schema used too few times.");
        verify(eventBus, never()).publish(any());
    }
}
