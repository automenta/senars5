package com.senars.systems;

import com.senars.core.*;
import com.senars.db.DatabaseManager;
import com.senars.systems.graphdb.MapDBGraphStore;
import com.senars.systems.vectorstore.DefaultVectorStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {

    @TempDir
    Path tempDir;
    DatabaseManager dbManager;
    GraphDB graphDB;
    VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test.db");
        dbManager = new DatabaseManager(dbFile);
        graphDB = new MapDBGraphStore(dbManager);
        vectorStore = new DefaultVectorStore(dbManager);
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
        graphDB.saveThought(thought1);
        vectorStore.add(thought1);

        // 2. Close the database to ensure data is flushed to disk
        dbManager.close();

        // 3. Re-open the database and stores
        Path dbFile = tempDir.resolve("test.db");
        dbManager = new DatabaseManager(dbFile);
        graphDB = new MapDBGraphStore(dbManager);
        vectorStore = new DefaultVectorStore(dbManager);

        // 4. Verify the thought is still there
        Optional<Thought> retrievedThought = graphDB.getThoughtById("thought1");
        assertTrue(retrievedThought.isPresent(), "Thought should be present after reloading DB");
        assertEquals("Test content", retrievedThought.get().content().text());

        // 5. Verify the vector is still there
        List<String> similarIds = vectorStore.findSimilar(List.of(0.1, 0.2), 1);
        assertFalse(similarIds.isEmpty(), "Vector search should find the thought");
        assertEquals("thought1", similarIds.getFirst());
    }
}
