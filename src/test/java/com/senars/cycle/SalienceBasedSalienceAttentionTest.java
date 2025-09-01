package com.senars.cycle;

import com.senars.core.*;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalienceBasedSalienceAttentionTest {

    @Mock
    private SalienceCalculator salienceCalculator;

    @Mock
    private MotiveHierarchy motiveHierarchy;

    private SalienceBasedAttention attentionFunnel;

    @BeforeEach
    void setUp() {
        attentionFunnel = new SalienceBasedAttention(salienceCalculator, motiveHierarchy);
    }

    private Thought createTestThought(String id, double activation) {
        return new Thought(
                id,
                new ThoughtContent("Test content for " + id, null, List.of(1.0, 2.0), null, null, null),
                new ThoughtState(1.0, 0.0, activation), // clarity, salience (not used here), activation
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.PERCEPTION, List.of(), Instant.now())
        );
    }

    @Test
    void shouldSelectThoughtWithHighestSalience() {
        // Arrange
        Thought thought1 = createTestThought("id1", 0.1); // Low salience
        Thought thought2 = createTestThought("id2", 0.9); // High salience
        Thought thought3 = createTestThought("id3", 0.5); // Medium salience

        // Configure mock SalienceCalculator
        when(salienceCalculator.calculate(thought1, motiveHierarchy)).thenReturn(10.0);
        when(salienceCalculator.calculate(thought2, motiveHierarchy)).thenReturn(100.0);
        when(salienceCalculator.calculate(thought3, motiveHierarchy)).thenReturn(50.0);

        attentionFunnel.addCandidate(thought1);
        attentionFunnel.addCandidate(thought2);
        attentionFunnel.addCandidate(thought3);

        // Act
        Optional<Thought> selectedThoughtOpt = attentionFunnel.selectFocusThought();

        // Assert
        assertTrue(selectedThoughtOpt.isPresent(), "A thought should have been selected");
        assertEquals("id2", selectedThoughtOpt.get().id(), "The thought with the highest salience should be selected");

        // Act again to see if the next highest is selected
        Optional<Thought> nextSelectedThoughtOpt = attentionFunnel.selectFocusThought();
        assertTrue(nextSelectedThoughtOpt.isPresent(), "A second thought should have been selected");
        assertEquals("id3", nextSelectedThoughtOpt.get().id(), "The thought with the second highest salience should be selected");

        // And the last one
        Optional<Thought> lastSelectedThoughtOpt = attentionFunnel.selectFocusThought();
        assertTrue(lastSelectedThoughtOpt.isPresent(), "A third thought should have been selected");
        assertEquals("id1", lastSelectedThoughtOpt.get().id(), "The thought with the lowest salience should be selected");

        // Funnel should be empty now
        Optional<Thought> emptyOpt = attentionFunnel.selectFocusThought();
        assertFalse(emptyOpt.isPresent(), "Funnel should be empty after all thoughts are selected");
    }

    @Test
    void shouldReturnEmptyOptionalWhenNoCandidates() {
        // Act
        Optional<Thought> selectedThoughtOpt = attentionFunnel.selectFocusThought();

        // Assert
        assertFalse(selectedThoughtOpt.isPresent(), "Should return empty optional when there are no candidate thoughts");
    }
}
