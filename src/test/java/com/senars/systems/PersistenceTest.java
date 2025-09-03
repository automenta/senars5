package com.senars.systems;

import com.senars.core.*;
import com.senars.db.DatabaseManager;
import com.senars.systems.graphdb.MapDBGraphStore;
import com.senars.systems.vectorstore.DefaultVectorStore;
import com.senars.systems.vectorstore.ScoredId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import com.senars.systems.memory.DefaultMemory;

class PersistenceTest {

    @TempDir
    Path tempDir;
    DatabaseManager dbManager;
    Memory memory;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test.db");
        dbManager = new DatabaseManager(dbFile);
        memory = new DefaultMemory(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    void testGraphAndVectorStorePersistence() {
        // 1. Create and save a thought
        Thought thought1 = new Thought(
                "thought1",
                new ThoughtContent("Test content", null, List.of(0.1, 0.2), null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, List.of(), Instant.now())
        );
        memory.saveThought(thought1);

        // 2. Persist and close the database to ensure data is flushed to disk
        memory.persist();
        dbManager.close();

        // 3. Re-open the database and memory
        Path dbFile = tempDir.resolve("test.db");
        dbManager = new DatabaseManager(dbFile);
        memory = new DefaultMemory(dbManager);


        // 4. Verify the thought is still there
        Optional<Thought> retrievedThought = memory.getThoughtById("thought1");
        assertTrue(retrievedThought.isPresent(), "Thought should be present after reloading DB");
        assertEquals("Test content", retrievedThought.get().content().text());

        // 5. Verify the vector is still there
        List<ScoredThought> similarThoughts = memory.retrieveSimilar(List.of(0.1, 0.2), 1);
        assertFalse(similarThoughts.isEmpty(), "Vector search should find the thought");
        assertEquals("thought1", similarThoughts.getFirst().thought().id());
    }
}
