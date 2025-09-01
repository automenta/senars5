package com.senars;

import com.senars.core.*;
import com.senars.cycle.Inference;
import com.senars.systems.Memory;
import com.senars.systems.immemory.InMemoryMemory;
import com.senars.config.AppConfig;
import com.senars.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class InferenceIntegrationTest {

    private Memory memory;
    private Inference inference;
    private DatabaseManager dbManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path dbFile = tempDir.resolve("test-inference.db");
        dbManager = new DatabaseManager(dbFile);
        memory = new InMemoryMemory(AppConfig.getInstance(), dbManager);
        inference = new Inference(memory);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    private Thought createBelief(String symbolicFact) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Fact: " + symbolicFact, symbolicFact, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, List.of(), Instant.now())
        );
    }

    private Thought createRuleSchema(List<String> rules) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Logical rules", "senars:family_rules", null, null, null, null, rules),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void testLogicalInferenceForGrandparent() {
        // 1. Setup Facts (Beliefs)
        memory.saveThought(createBelief("parent(john, paul)"));
        memory.saveThought(createBelief("parent(paul, mary)"));
        memory.saveThought(createBelief("parent(sue, john)"));

        // 2. Setup Rules (Schema)
        List<String> rules = List.of(
                "grandparent(X, Z) :- parent(X, Y), parent(Y, Z)."
        );
        memory.saveThought(createRuleSchema(rules));

        // 3. Create a Query (Goal)
        Thought query = new Thought(
                "query-1",
                new ThoughtContent("Who is sue's grandparent?", "grandparent(sue, Who)", null, null, null, null, null),
                new ThoughtState(1.0, 100.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now())
        );

        // 4. Execute Inference
        List<Thought> results = inference.reason(query);

        // 5. Assert the results
        assertNotNull(results);
        assertEquals(1, results.size());

        Thought resultThought = results.get(0);
        assertEquals(ThoughtType.BELIEF, resultThought.metadata().type());
        assertEquals(ThoughtOrigin.LOGIC_INFERENCE, resultThought.metadata().origin());

        // The symbolic part should be the grounded fact
        assertEquals("grandparent(sue,paul)", resultThought.content().symbolic().replaceAll("\\s",""));
        assertTrue(resultThought.content().text().contains("paul"));
    }
}
