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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttentionFunnelTest {

    @Mock
    private SalienceCalculator salienceCalculator;

    @Mock
    private MotiveHierarchy motiveHierarchy;

    private AttentionFunnel attentionFunnel;

    @BeforeEach
    void setUp() {
        attentionFunnel = new AttentionFunnel(salienceCalculator, motiveHierarchy);
    }

    private Thought createTestThought(String id) {
        return new Thought(
            id,
            new ThoughtContent("test content for " + id, null, null, null, null, null),
            new ThoughtState(1.0, 1.0, 1.0),
            new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void selectFocusThought_returnsEmptyWhenNoCandidates() {
        Optional<Thought> result = attentionFunnel.selectFocusThought();
        assertTrue(result.isEmpty());
    }

    @Test
    void selectFocusThought_returnsThoughtWithHighestSalience() {
        Thought thought1 = createTestThought("thought1"); // low salience
        Thought thought2 = createTestThought("thought2"); // high salience
        Thought thought3 = createTestThought("thought3"); // medium salience

        attentionFunnel.addCandidate(thought1);
        attentionFunnel.addCandidate(thought2);
        attentionFunnel.addCandidate(thought3);

        when(salienceCalculator.calculate(thought1, motiveHierarchy)).thenReturn(10.0);
        when(salienceCalculator.calculate(thought2, motiveHierarchy)).thenReturn(100.0);
        when(salienceCalculator.calculate(thought3, motiveHierarchy)).thenReturn(50.0);

        Optional<Thought> result = attentionFunnel.selectFocusThought();

        assertTrue(result.isPresent());
        assertEquals(thought2, result.get());
    }

    @Test
    void selectFocusThought_removesThoughtAfterSelection() {
        Thought thought1 = createTestThought("thought1");
        Thought thought2 = createTestThought("thought2");

        attentionFunnel.addCandidate(thought1);
        attentionFunnel.addCandidate(thought2);

        when(salienceCalculator.calculate(thought1, motiveHierarchy)).thenReturn(10.0);
        when(salienceCalculator.calculate(thought2, motiveHierarchy)).thenReturn(100.0);

        // First selection should be thought2
        Optional<Thought> result1 = attentionFunnel.selectFocusThought();
        assertTrue(result1.isPresent());
        assertEquals(thought2, result1.get());

        // Second selection should be thought1
        Optional<Thought> result2 = attentionFunnel.selectFocusThought();
        assertTrue(result2.isPresent());
        assertEquals(thought1, result2.get());

        // Funnel should now be empty
        Optional<Thought> result3 = attentionFunnel.selectFocusThought();
        assertTrue(result3.isEmpty());
    }

    @Test
    void addCandidate_doesNotAddDuplicates() {
        Thought thought1 = createTestThought("thought1");
        attentionFunnel.addCandidate(thought1);
        attentionFunnel.addCandidate(thought1); // Add the same thought again

        lenient().when(salienceCalculator.calculate(thought1, motiveHierarchy)).thenReturn(10.0);

        // Select the thought
        attentionFunnel.selectFocusThought();

        // The funnel should now be empty, proving the duplicate was not added
        Optional<Thought> result = attentionFunnel.selectFocusThought();
        assertTrue(result.isEmpty());
    }
}
