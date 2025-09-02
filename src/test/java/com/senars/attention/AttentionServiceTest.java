package com.senars.attention;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.motive.MotiveHierarchy;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttentionServiceTest {

    @Mock
    private Memory memory;
    @Mock
    private SalienceCalculator salienceCalculator;
    @Mock
    private UnifiedCausalReasoner ucr;
    @Mock
    private MotiveHierarchy motiveHierarchy;
    @Mock
    private EventBus eventBus;

    private AttentionService attentionService;

    @BeforeEach
    void setUp() {
        attentionService = new AttentionService(memory, salienceCalculator, ucr, motiveHierarchy, eventBus);
        // Default lenient stubbing for UCR to avoid errors in tests that don't focus on it
        lenient().when(ucr.reason(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void testSelectFocusThoughtWithNoCandidates() {
        // Act
        Optional<Thought> result = attentionService.selectFocusThought();

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testSelectFocusThoughtWithSingleCandidate() {
        // Arrange
        Thought thought = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Test thought", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
        attentionService.addCandidate(thought);

        when(salienceCalculator.calculate(thought, motiveHierarchy)).thenReturn(0.5);

        // Act
        Optional<Thought> result = attentionService.selectFocusThought();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(thought.id(), result.get().id());
    }

    @Test
    void testSelectFocusThoughtWithMultipleCandidates() {
        // Arrange
        Thought thought1 = new Thought(
                "id1",
                new ThoughtContent("Low salience", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
        Thought thought2 = new Thought(
                "id2",
                new ThoughtContent("High salience", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );

        attentionService.addCandidate(thought1);
        attentionService.addCandidate(thought2);

        when(salienceCalculator.calculate(thought1, motiveHierarchy)).thenReturn(0.2);
        when(salienceCalculator.calculate(thought2, motiveHierarchy)).thenReturn(0.8);

        // Act
        Optional<Thought> result = attentionService.selectFocusThought();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(thought2.id(), result.get().id());
    }
}
