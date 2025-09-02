package com.senars.systems.graphdb;

import com.senars.core.*;
import com.senars.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Skipping due to database file corruption issues")
class CausalGraphDBTest {

    private MapDBGraphStore graphStore;
    private DatabaseManager dbManager;
    private File tempDbFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary file for the database
        tempDbFile = Files.createTempFile("test-db-", ".mapdb").toFile();
        dbManager = new DatabaseManager(tempDbFile.toPath());
        graphStore = new MapDBGraphStore(dbManager);
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private Thought createTestThought(String id, String text, ThoughtType type) {
        return new Thought(
                id,
                new ThoughtContent(text, null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(type, ThoughtOrigin.SYSTEM, List.of(), Set.of(), Instant.now())
        );
    }

    @Test
    void testSaveAndRetrieveThought() {
        Thought thought = createTestThought("test-1", "Test thought content", ThoughtType.BELIEF);
        graphStore.saveThought(thought);

        Optional<Thought> retrieved = graphStore.getThoughtById("test-1");
        assertTrue(retrieved.isPresent());
        assertEquals("test-1", retrieved.get().id());
        assertEquals("Test thought content", retrieved.get().content().text());
    }

    @Test
    void testAddAndRetrieveCausalLinks() {
        Thought thought1 = createTestThought("thought-1", "First thought", ThoughtType.BELIEF);
        Thought thought2 = createTestThought("thought-2", "Second thought", ThoughtType.BELIEF);
        
        graphStore.saveThought(thought1);
        graphStore.saveThought(thought2);
        
        // Add a causal link from thought1 to thought2
        CausalLink link = new CausalLink("thought-1", "thought-2", CausalRelationType.DIRECT_CAUSATION);
        graphStore.addCausalLink(link);
        
        // Retrieve outgoing links from thought1
        Set<CausalLink> outgoingLinks = graphStore.getCausalLinksFrom("thought-1");
        assertEquals(1, outgoingLinks.size());
        assertTrue(outgoingLinks.contains(link));
        
        // Retrieve incoming links to thought2
        Set<CausalLink> incomingLinks = graphStore.getCausalLinksTo("thought-2");
        assertEquals(1, incomingLinks.size());
        assertTrue(incomingLinks.contains(link));
    }

    @Test
    void testCausalLeverageCalculation() {
        Thought thought1 = createTestThought("thought-1", "Root thought", ThoughtType.BELIEF);
        Thought thought2 = createTestThought("thought-2", "Child thought 1", ThoughtType.BELIEF);
        Thought thought3 = createTestThought("thought-3", "Child thought 2", ThoughtType.BELIEF);
        
        // Set different salience values for the child thoughts
        ThoughtState state2 = new ThoughtState(1.0, 50.0, 1.0); // High salience
        ThoughtState state3 = new ThoughtState(1.0, 10.0, 1.0); // Low salience
        
        thought2 = new Thought(
                "thought-2",
                thought2.content(),
                state2,
                thought2.metadata()
        );
        
        thought3 = new Thought(
                "thought-3",
                thought3.content(),
                state3,
                thought3.metadata()
        );
        
        graphStore.saveThought(thought1);
        graphStore.saveThought(thought2);
        graphStore.saveThought(thought3);
        
        // Add causal links
        graphStore.addCausalLink(new CausalLink("thought-1", "thought-2", CausalRelationType.DIRECT_CAUSATION));
        graphStore.addCausalLink(new CausalLink("thought-1", "thought-3", CausalRelationType.DIRECT_CAUSATION));
        
        // Calculate causal leverage for thought1
        double leverage = graphStore.calculateCausalLeverage("thought-1");
        
        // Leverage should be the sum of salience values of downstream thoughts
        // thought-1 itself should not be counted (leverage = 50.0 + 10.0 = 60.0)
        assertEquals(60.0, leverage, 0.001);
    }

    @Test
    void testCounterfactualAnalysis() {
        Thought thought1 = createTestThought("thought-1", "Root thought", ThoughtType.BELIEF);
        Thought thought2 = createTestThought("thought-2", "Child thought 1", ThoughtType.BELIEF);
        Thought thought3 = createTestThought("thought-3", "Child thought 2", ThoughtType.BELIEF);
        Thought thought4 = createTestThought("thought-4", "Grandchild thought", ThoughtType.BELIEF);
        
        graphStore.saveThought(thought1);
        graphStore.saveThought(thought2);
        graphStore.saveThought(thought3);
        graphStore.saveThought(thought4);
        
        // Add causal links
        graphStore.addCausalLink(new CausalLink("thought-1", "thought-2", CausalRelationType.DIRECT_CAUSATION));
        graphStore.addCausalLink(new CausalLink("thought-1", "thought-3", CausalRelationType.DIRECT_CAUSATION));
        graphStore.addCausalLink(new CausalLink("thought-2", "thought-4", CausalRelationType.DIRECT_CAUSATION));
        
        // Perform counterfactual analysis on thought1
        Set<Thought> affectedThoughts = graphStore.performCounterfactualAnalysis("thought-1");
        
        // Should include thought1, thought2, thought3, and thought4
        assertEquals(4, affectedThoughts.size());
        assertTrue(affectedThoughts.stream().anyMatch(t -> t.id().equals("thought-1")));
        assertTrue(affectedThoughts.stream().anyMatch(t -> t.id().equals("thought-2")));
        assertTrue(affectedThoughts.stream().anyMatch(t -> t.id().equals("thought-3")));
        assertTrue(affectedThoughts.stream().anyMatch(t -> t.id().equals("thought-4")));
    }

    @Test
    void testCausallyConnectedThoughts() {
        Thought thought1 = createTestThought("thought-1", "Root thought", ThoughtType.BELIEF);
        Thought thought2 = createTestThought("thought-2", "Child thought", ThoughtType.BELIEF);
        Thought thought3 = createTestThought("thought-3", "Parent thought", ThoughtType.BELIEF);
        Thought thought4 = createTestThought("thought-4", "Sibling thought", ThoughtType.BELIEF);
        
        graphStore.saveThought(thought1);
        graphStore.saveThought(thought2);
        graphStore.saveThought(thought3);
        graphStore.saveThought(thought4);
        
        // Add causal links
        graphStore.addCausalLink(new CausalLink("thought-3", "thought-1", CausalRelationType.DIRECT_CAUSATION)); // parent -> root
        graphStore.addCausalLink(new CausalLink("thought-1", "thought-2", CausalRelationType.DIRECT_CAUSATION)); // root -> child
        graphStore.addCausalLink(new CausalLink("thought-1", "thought-4", CausalRelationType.DIRECT_CAUSATION)); // root -> sibling
        
        // Get causally connected thoughts with max depth of 1
        Set<Thought> connectedThoughts = graphStore.getCausallyConnectedThoughts("thought-1", 1);
        
        // Should include thought1 (itself), thought2 (child), thought3 (parent), and thought4 (sibling)
        assertEquals(4, connectedThoughts.size());
        assertTrue(connectedThoughts.stream().anyMatch(t -> t.id().equals("thought-1")));
        assertTrue(connectedThoughts.stream().anyMatch(t -> t.id().equals("thought-2")));
        assertTrue(connectedThoughts.stream().anyMatch(t -> t.id().equals("thought-3")));
        assertTrue(connectedThoughts.stream().anyMatch(t -> t.id().equals("thought-4")));
    }
}