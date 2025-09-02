package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.senars.events.EventBus;

@ExtendWith(MockitoExtension.class)
class InMemoryGroundingSystemTest {

    @Mock
    private Memory memory;
    @Mock
    private EventBus eventBus;

    private InMemoryGrounding grounding;

    @BeforeEach
    void setUp() {
        // Use the constructor that allows setting both factors for predictable tests
        grounding = new InMemoryGrounding(memory, eventBus, 0.1, 0.1);
    }

    private Thought createTestThought(String id, double clarity) {
        return new Thought(
                id,
                new ThoughtContent("text", "symbolic", null, null, null, null, null),
                new ThoughtState(clarity, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, Collections.emptyList(), Instant.now())
        );
    }

    private Feedback createFeedback(List<String> trace, ActionStatus status) {
        Thought actionPlan = new Thought(
                "action-1",
                new ThoughtContent("text", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION_PLAN, ThoughtOrigin.LLM_INFERENCE, trace, Instant.now())
        );
        return new Feedback(status, "test.tool", "test observation", 100L, actionPlan);
    }

    @Test
    void processFeedback_withPositiveFeedback_increasesClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Feedback feedback = createFeedback(List.of(thought1.id()), ActionStatus.SUCCESS);

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        grounding.processFeedback(feedback);

        // Assert
        verify(memory).saveThought(argThat(thought -> thought.id().equals("thought1") && thought.state().clarity() > 0.5));
    }

    @Test
    void processFeedback_withNegativeFeedback_decreasesClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Feedback feedback = createFeedback(List.of("thought1"), ActionStatus.FAILURE);

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        grounding.processFeedback(feedback);

        // Assert
        verify(memory).saveThought(argThat(thought -> thought.id().equals("thought1") && thought.state().clarity() < 0.5));
    }


    @Test
    void processFeedback_withEmptyTrace_doesNothing() {
        // Arrange
        Thought thoughtWithEmptyTrace = new Thought(
                "id",
                new ThoughtContent("text", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, Collections.emptyList(), Instant.now())
        );
        Feedback feedback = createFeedback(Collections.emptyList(), ActionStatus.SUCCESS);


        // Act
        grounding.processFeedback(feedback);

        // Assert
        verify(memory, never()).getThoughtById(any());
        verify(memory, never()).saveThought(any());
    }

    @Test
    void processFeedback_withMultipleThoughtsInTrace_appliesDecayedAdjustment() {
        // Arrange
        grounding = new InMemoryGrounding(memory, eventBus, 0.1, 0.1); // Ensure symmetric factors for this test
        Thought thought1 = createTestThought("thought1", 0.5); // least recent
        Thought thought2 = createTestThought("thought2", 0.5);
        Thought thought3 = createTestThought("thought3", 0.5); // most recent
        Feedback feedback = createFeedback(List.of("thought1", "thought2", "thought3"), ActionStatus.SUCCESS);

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));
        when(memory.getThoughtById("thought2")).thenReturn(Optional.of(thought2));
        when(memory.getThoughtById("thought3")).thenReturn(Optional.of(thought3));

        ArgumentCaptor<Thought> thoughtCaptor = ArgumentCaptor.forClass(Thought.class);

        // Act
        grounding.processFeedback(feedback);

        // Assert
        verify(memory, times(3)).saveThought(thoughtCaptor.capture());

        List<Thought> savedThoughts = thoughtCaptor.getAllValues();
        savedThoughts.sort(Comparator.comparing(Thought::id)); // Sort by ID to ensure consistent order

        Thought savedThought1 = savedThoughts.get(0);
        Thought savedThought2 = savedThoughts.get(1);
        Thought savedThought3 = savedThoughts.get(2);

        // Expected clarity values:
        // initial adjustment = (1.0 - 0.5) * 2 * 0.1 = 0.1
        // thought3 (dist=0): 0.5 + 0.1 * (0.9^0) = 0.6
        // thought2 (dist=1): 0.5 + 0.1 * (0.9^1) = 0.59
        // thought1 (dist=2): 0.5 + 0.1 * (0.9^2) = 0.581

        assertEquals("thought1", savedThought1.id());
        assertEquals(0.581, savedThought1.state().clarity(), 1e-9);

        assertEquals("thought2", savedThought2.id());
        assertEquals(0.59, savedThought2.state().clarity(), 1e-9);

        assertEquals("thought3", savedThought3.id());
        assertEquals(0.6, savedThought3.state().clarity(), 1e-9);

        // Also assert the order of clarity increase
        assertTrue(savedThought3.state().clarity() > savedThought2.state().clarity());
        assertTrue(savedThought2.state().clarity() > savedThought1.state().clarity());
    }

    @Test
    void processFeedback_withAsymmetricFactors_appliesDifferentAdjustments() {
        // Arrange
        grounding = new InMemoryGrounding(memory, eventBus, 0.1, 0.4); // 0.1 for success, 0.4 for failure
        Thought successThought = createTestThought("success_thought", 0.5);
        Thought failureThought = createTestThought("failure_thought", 0.5);
        ArgumentCaptor<Thought> thoughtCaptor = ArgumentCaptor.forClass(Thought.class);

        // Act for success
        when(memory.getThoughtById("success_thought")).thenReturn(Optional.of(successThought));
        grounding.processFeedback(createFeedback(List.of(successThought.id()), ActionStatus.SUCCESS));

        // Act for failure
        when(memory.getThoughtById("failure_thought")).thenReturn(Optional.of(failureThought));
        grounding.processFeedback(createFeedback(List.of(failureThought.id()), ActionStatus.FAILURE));

        // Assert
        verify(memory, times(2)).saveThought(thoughtCaptor.capture());
        List<Thought> savedThoughts = thoughtCaptor.getAllValues();
        Thought savedSuccess = savedThoughts.stream().filter(t -> t.id().equals("success_thought")).findFirst().get();
        Thought savedFailure = savedThoughts.stream().filter(t -> t.id().equals("failure_thought")).findFirst().get();

        // Expected success clarity: 0.5 + (1.0 - 0.5) * 2 * 0.1 = 0.5 + 0.1 = 0.6
        assertEquals(0.6, savedSuccess.state().clarity(), 1e-9);

        // Expected failure clarity: 0.5 + (0.0 - 0.5) * 2 * 0.4 = 0.5 - 0.4 = 0.1
        assertEquals(0.1, savedFailure.state().clarity(), 1e-9);
    }
}
