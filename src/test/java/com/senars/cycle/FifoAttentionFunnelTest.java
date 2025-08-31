package com.senars.cycle;

import com.senars.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FifoAttentionFunnelTest {

    private IAttentionFunnel fifoFunnel;

    @BeforeEach
    void setUp() {
        fifoFunnel = new FifoAttentionFunnel();
    }

    private Thought createTestThought(String id) {
        return new Thought(
            id,
            new ThoughtContent("test", null, null, null, null),
            new ThoughtState(1.0, 1.0, 1.0),
            new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );
    }

    @Test
    void selectFocusThought_shouldReturnThoughtsInFifoOrder() {
        // Arrange
        Thought thought1 = createTestThought("thought-1");
        Thought thought2 = createTestThought("thought-2");

        fifoFunnel.addCandidate(thought1);
        fifoFunnel.addCandidate(thought2);

        // Act & Assert
        Optional<Thought> result1 = fifoFunnel.selectFocusThought();
        assertTrue(result1.isPresent());
        assertEquals("thought-1", result1.get().id());

        Optional<Thought> result2 = fifoFunnel.selectFocusThought();
        assertTrue(result2.isPresent());
        assertEquals("thought-2", result2.get().id());
    }

    @Test
    void selectFocusThought_whenEmpty_shouldReturnEmptyOptional() {
        // Act
        Optional<Thought> result = fifoFunnel.selectFocusThought();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void addCandidate_withNull_shouldNotThrowExceptionAndFunnelRemainsEmpty() {
        // Arrange
        fifoFunnel.addCandidate(null);

        // Act
        Optional<Thought> result = fifoFunnel.selectFocusThought();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void selectFocusThought_afterAddingAndRemoving_shouldBeEmpty() {
        // Arrange
        Thought thought1 = createTestThought("thought-1");
        fifoFunnel.addCandidate(thought1);

        // Act
        fifoFunnel.selectFocusThought(); // remove the thought
        Optional<Thought> result = fifoFunnel.selectFocusThought(); // try to get another

        // Assert
        assertTrue(result.isEmpty());
    }
}
