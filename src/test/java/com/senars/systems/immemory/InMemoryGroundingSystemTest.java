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

@ExtendWith(MockitoExtension.class)
class InMemoryGroundingSystemTest {

    @Mock
    private Memory memory;

    private InMemoryGrounding grounding;

    @BeforeEach
    void setUp() {
        grounding = new InMemoryGrounding(memory, 0.1); // Using a known adjustment factor
    }

    private Thought createTestThought(String id, double clarity) {
        return new Thought(
                id,
                new ThoughtContent("text", "symbolic", null, null, null, null),
                new ThoughtState(clarity, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, Collections.emptyList(), Instant.now())
        );
    }

    private Thought createFeedbackReport(List<String> trace, double success) {
        Feedback feedback = new Feedback(success, "test feedback");
        return new Thought(
                "feedback-report",
                new ThoughtContent("report", null, null, null, null, feedback),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.REPORT, ThoughtOrigin.SYSTEM, trace, Instant.now())
        );
    }

    @Test
    void processFeedback_withPositiveFeedback_increasesClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Thought feedbackReport = createFeedbackReport(List.of("thought1"), 1.0); // 1.0 is success

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        grounding.processFeedback(feedbackReport);

        // Assert
        verify(memory).saveThought(argThat(thought -> thought.id().equals("thought1") && thought.state().clarity() > 0.5));
    }

    @Test
    void processFeedback_withNegativeFeedback_decreasesClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Thought feedbackReport = createFeedbackReport(List.of("thought1"), 0.0); // 0.0 is failure

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        grounding.processFeedback(feedbackReport);

        // Assert
        verify(memory).saveThought(argThat(thought -> thought.id().equals("thought1") && thought.state().clarity() < 0.5));
    }

    @Test
    void processFeedback_withNeutralFeedback_doesNotChangeClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Thought feedbackReport = createFeedbackReport(List.of("thought1"), 0.5); // 0.5 is neutral

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        grounding.processFeedback(feedbackReport);

        // Assert
        verify(memory, never()).saveThought(any());
    }

    @Test
    void processFeedback_withEmptyTrace_doesNothing() {
        // Arrange
        Thought feedbackReport = createFeedbackReport(Collections.emptyList(), 1.0);

        // Act
        grounding.processFeedback(feedbackReport);

        // Assert
        verify(memory, never()).getThoughtById(any());
        verify(memory, never()).saveThought(any());
    }

    @Test
    void processFeedback_withNullFeedback_doesNothing() {
        // Arrange
        Thought feedbackReport = new Thought(
                "report",
                new ThoughtContent(null, null, null, null, null, null), // Null feedback
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.REPORT, ThoughtOrigin.SYSTEM, List.of("id1"), Instant.now())
        );

        // Act
        grounding.processFeedback(feedbackReport);

        // Assert
        verify(memory, never()).getThoughtById(any());
    }

    @Test
    void processFeedback_withMultipleThoughtsInTrace_appliesDecayedAdjustment() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5); // least recent
        Thought thought2 = createTestThought("thought2", 0.5);
        Thought thought3 = createTestThought("thought3", 0.5); // most recent
        List<String> trace = List.of("thought1", "thought2", "thought3");
        Thought feedbackReport = createFeedbackReport(trace, 1.0); // Full success

        when(memory.getThoughtById("thought1")).thenReturn(Optional.of(thought1));
        when(memory.getThoughtById("thought2")).thenReturn(Optional.of(thought2));
        when(memory.getThoughtById("thought3")).thenReturn(Optional.of(thought3));

        ArgumentCaptor<Thought> thoughtCaptor = ArgumentCaptor.forClass(Thought.class);

        // Act
        grounding.processFeedback(feedbackReport);

        // Assert
        verify(memory, times(3)).saveThought(thoughtCaptor.capture());

        List<Thought> savedThoughts = thoughtCaptor.getAllValues();
        savedThoughts.sort(Comparator.comparing(Thought::id)); // Sort by ID to ensure consistent order

        Thought savedThought1 = savedThoughts.get(0);
        Thought savedThought2 = savedThoughts.get(1);
        Thought savedThought3 = savedThoughts.get(2);

        // Expected clarity values:
        // initial adjustment = (1.0 - 0.5) * 0.1 = 0.05
        // thought3 (dist=0): 0.5 + 0.05 * (0.9^0) = 0.55
        // thought2 (dist=1): 0.5 + 0.05 * (0.9^1) = 0.545
        // thought1 (dist=2): 0.5 + 0.05 * (0.9^2) = 0.5405

        assertEquals("thought1", savedThought1.id());
        assertEquals(0.5405, savedThought1.state().clarity(), 1e-9);

        assertEquals("thought2", savedThought2.id());
        assertEquals(0.545, savedThought2.state().clarity(), 1e-9);

        assertEquals("thought3", savedThought3.id());
        assertEquals(0.55, savedThought3.state().clarity(), 1e-9);

        // Also assert the order of clarity increase
        assertTrue(savedThought3.state().clarity() > savedThought2.state().clarity());
        assertTrue(savedThought2.state().clarity() > savedThought1.state().clarity());
    }
}
