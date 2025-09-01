package com.senars.systems;

import com.senars.core.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PersistentMemoryNexusTest {

    @TempDir
    Path tempDir;
    private PersistentMemory memoryNexus;
    private EmbeddingModel mockEmbeddingModel;
    private Path graphDbPath;
    private Path embeddingStorePath;

    @BeforeEach
    void setUp() {
        graphDbPath = tempDir.resolve("test_graphdb.json");
        embeddingStorePath = tempDir.resolve("test_embedding_store.json");

        mockEmbeddingModel = Mockito.mock(EmbeddingModel.class);
        // This is the embedding that the mock model will return.
        // The FileBasedEmbeddingStore uses this to create an embedding for the text.
        // The test then creates a Thought with a *different* embedding to ensure the one from the thought is saved.
        Embedding mockModelEmbedding = Embedding.from(Arrays.asList(9.9f, 9.8f, 9.7f));
        when(mockEmbeddingModel.embed(any(TextSegment.class))).thenReturn(Response.from(mockModelEmbedding));

        memoryNexus = new PersistentMemory(graphDbPath.toString(), embeddingStorePath.toString(), mockEmbeddingModel);
    }

    @Test
    void testSaveAndRetrieveThoughtWithEmbedding() {
        // 1. Create a Thought with a specific embedding
        List<Double> originalEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        ThoughtContent content = new ThoughtContent("test text", "test:symbol", originalEmbedding, null, null, null);
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
        ThoughtMeta metadata = new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now());
        Thought originalThought = new Thought("test-id-1", content, state, metadata);

        // 2. Save the thought
        memoryNexus.saveThought(originalThought);

        // 3. Retrieve the thought
        Optional<Thought> retrievedThoughtOpt = memoryNexus.getThoughtById("test-id-1");

        // 4. Assertions
        assertTrue(retrievedThoughtOpt.isPresent(), "Thought should be found");
        Thought retrievedThought = retrievedThoughtOpt.get();

        assertEquals(originalThought.id(), retrievedThought.id());
        assertNotNull(retrievedThought.content().embedding(), "Embedding should not be null");
        assertEquals(originalEmbedding.size(), retrievedThought.content().embedding().size(), "Embedding size should match");
        assertEquals(originalEmbedding, retrievedThought.content().embedding(), "Embedding content should match the original thought's embedding");
    }

    @Test
    void testSaveAndRetrieveThoughtWithoutEmbedding() {
        // 1. Create a Thought with a null embedding
        ThoughtContent content = new ThoughtContent("test text without embedding", "test:symbol:no_embedding", null, null, null, null);
        ThoughtState state = new ThoughtState(0.5, 0.5, 0.5);
        ThoughtMeta metadata = new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now());
        Thought originalThought = new Thought("test-id-2", content, state, metadata);

        // 2. Save the thought
        memoryNexus.saveThought(originalThought);

        // 3. Retrieve the thought
        Optional<Thought> retrievedThoughtOpt = memoryNexus.getThoughtById("test-id-2");

        // 4. Assertions
        assertTrue(retrievedThoughtOpt.isPresent(), "Thought should be found");
        Thought retrievedThought = retrievedThoughtOpt.get();

        assertEquals(originalThought.id(), retrievedThought.id());
        assertNull(retrievedThought.content().embedding(), "Embedding should be null");
    }
}
