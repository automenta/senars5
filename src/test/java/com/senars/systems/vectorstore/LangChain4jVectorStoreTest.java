package com.senars.systems.vectorstore;

import com.senars.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LangChain4jVectorStoreTest {

    private LangChain4jVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new LangChain4jVectorStore();
    }

    private Thought createTestThoughtWithEmbedding(String id, List<Double> embedding) {
        return new Thought(
                id,
                new ThoughtContent("text", "symbolic", embedding, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );
    }

    @Test
    void testAddAndFindSimilar() {
        // Create two thoughts with distinct embeddings
        Thought thought1 = createTestThoughtWithEmbedding("T1", List.of(1.0, 0.0, 0.0));
        Thought thought2 = createTestThoughtWithEmbedding("T2", List.of(0.0, 1.0, 0.0));

        vectorStore.add(thought1);
        vectorStore.add(thought2);

        // Search for an embedding that is identical to thought2
        List<String> similarIds = vectorStore.findSimilar(List.of(0.0, 1.0, 0.0), 2);

        // The first result should be T2
        assertFalse(similarIds.isEmpty());
        assertEquals("T2", similarIds.getFirst());

        // The list should contain both IDs
        assertEquals(2, similarIds.size());
        assertTrue(similarIds.containsAll(List.of("T1", "T2")));
    }

    @Test
    void testAddThoughtWithoutEmbedding() {
        // This should not throw an error, it should just be ignored.
        Thought thought = createTestThoughtWithEmbedding("T3", null);
        assertDoesNotThrow(() -> vectorStore.add(thought));
    }

    @Test
    void testFindSimilarWithNoMatches() {
        List<String> similarIds = vectorStore.findSimilar(List.of(0.5, 0.5, 0.5), 1);
        assertTrue(similarIds.isEmpty());
    }

    @Test
    void testRemoveDoesNotThrowException() {
        // The remove method is a no-op and should not throw an exception.
        // It should log a warning, but we won't test for that here.
        assertDoesNotThrow(() -> vectorStore.remove("some_id"));
    }
}
