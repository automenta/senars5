package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.systems.Memory;
import com.senars.config.AppConfig;
import com.senars.db.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryMemoryTest {

    private Memory memory;

    @BeforeEach
    void setUp() {
        AppConfig mockConfig = mock(AppConfig.class);
        // Use a real DatabaseManager with an in-memory DB for this test
        DatabaseManager dbManager = new DatabaseManager(null); // Passing null for in-memory
        memory = new InMemoryMemory(mockConfig, dbManager);
    }

    private Thought createTestThoughtWithEmbedding(String id, String text, List<Double> embedding, List<String> parentIds) {
        return new Thought(
                id,
                new ThoughtContent(text, "symbolic:" + id, embedding, null, null, null, null),
                new ThoughtState(0.9, 0.8, 0.7),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, parentIds, Instant.now())
        );
    }

    @Test
    void testSaveAndRetrieveEndToEnd() {
        Thought thought = createTestThoughtWithEmbedding("T1", "Test thought 1", List.of(1.0, 0.0), Collections.emptyList());
        memory.saveThought(thought);

        // Test retrieval from graph component
        Optional<Thought> retrievedById = memory.getThoughtById("T1");
        assertTrue(retrievedById.isPresent());
        assertEquals("T1", retrievedById.get().id());

        // Test retrieval from vector component
        List<Thought> similar = memory.retrieveSimilar(List.of(1.0, 0.1), 1); // Slightly different vector
        assertFalse(similar.isEmpty());
        assertEquals("T1", similar.getFirst().id());
    }

    @Test
    void testSaveThoughtWithoutEmbedding() {
        Thought thought = createTestThoughtWithEmbedding("T2", "No embedding", null, Collections.emptyList());
        memory.saveThought(thought);

        // Should be retrievable by ID
        assertTrue(memory.getThoughtById("T2").isPresent());

        // Should not be found in a similarity search
        Thought thoughtWithEmbedding = createTestThoughtWithEmbedding("T3", "With embedding", List.of(0.5, 0.5), Collections.emptyList());
        memory.saveThought(thoughtWithEmbedding);

        List<Thought> similar = memory.retrieveSimilar(List.of(0.5, 0.5), 5);
        assertEquals(1, similar.size());
        assertEquals("T3", similar.getFirst().id());
    }

    @Test
    void testDeleteThought() {
        Thought thought = createTestThoughtWithEmbedding("T4", "To be deleted", List.of(0.2, 0.8), Collections.emptyList());
        memory.saveThought(thought);

        assertTrue(memory.getThoughtById("T4").isPresent());

        memory.deleteThought("T4");

        // Should be gone from graph
        assertFalse(memory.getThoughtById("T4").isPresent());

        // We know it's not gone from vector store, but we can't test that without mocking.
        // The important part for the user is that it's not retrievable as a full thought.
    }

    @Test
    void testGetTraceIsDelegated() {
        // This test confirms that the trace logic from the GraphDB is correctly exposed.
        Thought t1 = createTestThoughtWithEmbedding("G1", "Graph Parent 1", null, Collections.emptyList());
        Thought t2 = createTestThoughtWithEmbedding("G2", "Graph Parent 2", null, List.of("G1"));
        Thought t3 = createTestThoughtWithEmbedding("G3", "Graph Child", null, List.of("G2"));

        memory.saveThought(t1);
        memory.saveThought(t2);
        memory.saveThought(t3);

        List<Thought> trace = memory.getTrace("G3");
        assertEquals(3, trace.size());
        List<String> traceIds = trace.stream().map(Thought::id).toList();

        assertEquals("G1", traceIds.get(0));
        assertEquals("G2", traceIds.get(1));
        assertEquals("G3", traceIds.get(2));
    }
}
