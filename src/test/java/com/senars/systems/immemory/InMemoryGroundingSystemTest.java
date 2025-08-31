package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.systems.IMemoryNexus;
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
class InMemoryGroundingSystemTest {

    @Mock
    private IMemoryNexus memoryNexus;

    private InMemoryGroundingSystem groundingSystem;

    @BeforeEach
    void setUp() {
        groundingSystem = new InMemoryGroundingSystem(memoryNexus);
    }

    private Thought createTestThought(String id, double clarity) {
        return new Thought(
            id,
            new ThoughtContent("text", "symbolic", null, null, null, null),
            new ThoughtState(clarity, 1.0, 1.0),
            new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, Collections.emptyList(), Instant.now())
        );
    }

    private Thought createFeedbackReport(List<String> trace, double success) {
        Feedback feedback = new Feedback(success, "test feedback");
        return new Thought(
            "feedback-report",
            new ThoughtContent("report", null, null, null, null, feedback),
            new ThoughtState(1.0, 1.0, 1.0),
            new ThoughtMetadata(ThoughtType.REPORT, ThoughtOrigin.SYSTEM, trace, Instant.now())
        );
    }

    @Test
    void processFeedback_withPositiveFeedback_increasesClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Thought feedbackReport = createFeedbackReport(List.of("thought1"), 1.0); // 1.0 is success

        when(memoryNexus.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        groundingSystem.processFeedback(feedbackReport);

        // Assert
        verify(memoryNexus).saveThought(argThat(thought -> thought.id().equals("thought1") && thought.state().clarity() > 0.5));
    }

    @Test
    void processFeedback_withNegativeFeedback_decreasesClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Thought feedbackReport = createFeedbackReport(List.of("thought1"), 0.0); // 0.0 is failure

        when(memoryNexus.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        groundingSystem.processFeedback(feedbackReport);

        // Assert
        verify(memoryNexus).saveThought(argThat(thought -> thought.id().equals("thought1") && thought.state().clarity() < 0.5));
    }

    @Test
    void processFeedback_withNeutralFeedback_doesNotChangeClarity() {
        // Arrange
        Thought thought1 = createTestThought("thought1", 0.5);
        Thought feedbackReport = createFeedbackReport(List.of("thought1"), 0.5); // 0.5 is neutral

        when(memoryNexus.getThoughtById("thought1")).thenReturn(Optional.of(thought1));

        // Act
        groundingSystem.processFeedback(feedbackReport);

        // Assert
        verify(memoryNexus, never()).saveThought(any());
    }

    @Test
    void processFeedback_withEmptyTrace_doesNothing() {
        // Arrange
        Thought feedbackReport = createFeedbackReport(Collections.emptyList(), 1.0);

        // Act
        groundingSystem.processFeedback(feedbackReport);

        // Assert
        verify(memoryNexus, never()).getThoughtById(any());
        verify(memoryNexus, never()).saveThought(any());
    }

    @Test
    void processFeedback_withNullFeedback_doesNothing() {
        // Arrange
        Thought feedbackReport = new Thought(
            "report",
            new ThoughtContent(null, null, null, null, null, null), // Null feedback
            new ThoughtState(1.0, 1.0, 1.0),
            new ThoughtMetadata(ThoughtType.REPORT, ThoughtOrigin.SYSTEM, List.of("id1"), Instant.now())
        );

        // Act
        groundingSystem.processFeedback(feedbackReport);

        // Assert
        verify(memoryNexus, never()).getThoughtById(any());
    }
}
