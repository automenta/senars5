package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryGroundingSystemTest {

    @Mock
    private Memory memory;
    @Mock
    private EventBus eventBus;

    private InMemoryGrounding grounding;

    @BeforeEach
    void setUp() {
        grounding = new InMemoryGrounding(memory, eventBus, 0.1, 0.5);
    }

    private Thought createTestThought(String id, double clarity, ThoughtType type) {
        return new Thought(
                id,
                new ThoughtContent("text", "symbolic", null, null, null, null, null),
                new ThoughtState(clarity, 1.0, 1.0),
                new ThoughtMeta(type, ThoughtOrigin.LLM_INFERENCE, Collections.emptyList(), Instant.now())
        );
    }

    private Feedback createFeedback(Thought actionPlan) {
        return new Feedback(ActionStatus.FAILURE, "test.tool", "test output", 100L, actionPlan);
    }

    @Test
    void processFeedback_adjustsClarityOfActionPlanAndTrace() {
        // GIVEN
        Thought schema = createTestThought("schema-1", 0.8, ThoughtType.SCHEMA);
        Thought goal = createTestThought("goal-1", 0.9, ThoughtType.GOAL);
        Thought actionPlan = new Thought(
                "action-1",
                new ThoughtContent("action text", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION_PLAN, ThoughtOrigin.LLM_INFERENCE, List.of(goal.id(), schema.id()), Instant.now())
        );
        Feedback feedback = createFeedback(actionPlan);

        when(memory.getThoughtById("schema-1")).thenReturn(Optional.of(schema));
        when(memory.getThoughtById("goal-1")).thenReturn(Optional.of(goal));

        ArgumentCaptor<Thought> thoughtCaptor = ArgumentCaptor.forClass(Thought.class);

        // WHEN
        grounding.processFeedback(feedback);

        // THEN
        verify(memory, times(3)).saveThought(thoughtCaptor.capture());

        Thought savedActionPlan = thoughtCaptor.getAllValues().stream().filter(t -> t.id().equals("action-1")).findFirst().orElseThrow();
        Thought savedSchema = thoughtCaptor.getAllValues().stream().filter(t -> t.id().equals("schema-1")).findFirst().orElseThrow();
        Thought savedGoal = thoughtCaptor.getAllValues().stream().filter(t -> t.id().equals("goal-1")).findFirst().orElseThrow();

        // Check that clarities were reduced
        assertTrue(savedActionPlan.state().clarity() < actionPlan.state().clarity(), "Action plan clarity should decrease on failure.");
        assertTrue(savedSchema.state().clarity() < schema.state().clarity(), "Schema clarity should decrease on failure.");
        assertTrue(savedGoal.state().clarity() < goal.state().clarity(), "Goal clarity should decrease on failure.");

        // Check decay logic (schema is blamed more than the goal)
        double schemaReduction = schema.state().clarity() - savedSchema.state().clarity();
        double goalReduction = goal.state().clarity() - savedGoal.state().clarity();
        assertTrue(schemaReduction > goalReduction, "Schema should be blamed more (clarity reduced more) than the goal.");
    }
}
