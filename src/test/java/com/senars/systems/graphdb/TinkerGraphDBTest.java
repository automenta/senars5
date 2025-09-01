package com.senars.systems.graphdb;

import com.senars.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TinkerGraphDBTest {

    @TempDir
    Path tempDir;
    private TinkerGraphDB db;

    @BeforeEach
    void setUp() {
        File dbFile = tempDir.resolve("test-graph.json").toFile();
        db = new TinkerGraphDB(dbFile.getAbsolutePath());
    }

    private Thought createTestThought(String id, String text, List<String> parentIds) {
        return new Thought(
                id,
                new ThoughtContent(text, "symbolic:" + id, null, null, "procedural_content", null, null),
                new ThoughtState(0.9, 0.8, 0.7),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.LLM_INFERENCE, parentIds, Instant.now())
        );
    }

    @Test
    void testSaveAndGetThought() {
        Thought thought = createTestThought(UUID.randomUUID().toString(), "This is a test thought.", Collections.emptyList());
        db.saveThought(thought);

        Optional<Thought> retrieved = db.getThoughtById(thought.id());
        assertTrue(retrieved.isPresent());
        assertEquals(thought.id(), retrieved.get().id());
        assertEquals("This is a test thought.", retrieved.get().content().text());
        assertEquals("procedural_content", retrieved.get().content().procedural());
    }

    @Test
    void testUpdateThought() {
        String id = UUID.randomUUID().toString();
        Thought originalThought = createTestThought(id, "Original text.", Collections.emptyList());
        db.saveThought(originalThought);

        Thought updatedThought = new Thought(
                id,
                new ThoughtContent("Updated text.", "symbolic:updated", null, null, null, null, null),
                new ThoughtState(0.5, 0.5, 0.5),
                originalThought.metadata()
        );
        db.saveThought(updatedThought);

        Optional<Thought> retrieved = db.getThoughtById(id);
        assertTrue(retrieved.isPresent());
        assertEquals("Updated text.", retrieved.get().content().text());
        assertEquals(0.5, retrieved.get().state().clarity());
    }

    @Test
    void testDeleteThought() {
        Thought thought = createTestThought(UUID.randomUUID().toString(), "To be deleted.", Collections.emptyList());
        db.saveThought(thought);
        assertTrue(db.getThoughtById(thought.id()).isPresent());

        db.deleteThought(thought.id());
        assertFalse(db.getThoughtById(thought.id()).isPresent());
    }

    @Test
    void testFindSchemaBySymbolicName() {
        Thought schema = new Thought(
                "schema1",
                new ThoughtContent("A test schema", "senars:test_schema_v1", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );
        db.saveThought(schema);

        Optional<Thought> found = db.findSchemaBySymbolicName("senars:test_schema_v1");
        assertTrue(found.isPresent());
        assertEquals("schema1", found.get().id());
    }

    @Test
    void testGetTraceComplex() {
        // Build a graph: T4 -> T3 -> (T1, T2)
        Thought t1 = createTestThought("T1", "Parent 1", Collections.emptyList());
        Thought t2 = createTestThought("T2", "Parent 2", Collections.emptyList());
        Thought t3 = createTestThought("T3", "Child of T1 and T2", List.of("T1", "T2"));
        Thought t4 = createTestThought("T4", "Child of T3", List.of("T3"));

        db.saveThought(t1);
        db.saveThought(t2);
        db.saveThought(t3);
        db.saveThought(t4);

        List<Thought> trace = db.getTrace("T4");

        // Expected size is 4 (T1, T2, T3, T4)
        assertEquals(4, trace.size());

        // The trace should be topologically sorted. T4 must be last. T3 must be before T4. T1/T2 must be before T3.
        List<String> traceIds = trace.stream().map(Thought::id).toList();

        assertTrue(traceIds.indexOf("T3") < traceIds.indexOf("T4"), "T3 should come before T4");
        assertTrue(traceIds.indexOf("T1") < traceIds.indexOf("T3"), "T1 should come before T3");
        assertTrue(traceIds.indexOf("T2") < traceIds.indexOf("T3"), "T2 should come before T3");

        // Check that all expected thoughts are present
        assertTrue(traceIds.containsAll(List.of("T1", "T2", "T3", "T4")));
    }

    @Test
    void testGetTraceWithMissingNode() {
        List<Thought> trace = db.getTrace("nonexistent_id");
        assertTrue(trace.isEmpty());
    }
}
