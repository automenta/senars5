package com.senars;

import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.llm.Langchain4jCognitiveProcessor;
import com.senars.llm.PromptBuilder;
import com.senars.llm.StructuredOutputParser;
import com.senars.systems.IMemoryNexus;
import com.senars.systems.immemory.InMemoryMemoryNexus;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the SeNARS system that wires up real components
 * and interacts with a live LLM.
 */
@Disabled("Requires a running Ollama instance")
public class IntegrationTest {

    private Langchain4jCognitiveProcessor cognitiveProcessor;

    @BeforeEach
    void setUp() {
        // 1. Load configuration
        AppConfig config = AppConfig.getInstance();

        // 2. Set up the real ChatModel
        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName())
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout()))
                .build();

        // 3. Set up real dependencies
        IMemoryNexus memoryNexus = new InMemoryMemoryNexus();
        PromptBuilder promptBuilder = new PromptBuilder();
        StructuredOutputParser outputParser = new StructuredOutputParser();

        // 4. Instantiate the real processor
        cognitiveProcessor = new Langchain4jCognitiveProcessor(
                chatModel,
                memoryNexus,
                promptBuilder,
                outputParser
        );
    }

    @Test
    void testProcessThoughtWithRealLlm() {
        // This test requires a running Ollama instance with the specified model.
        // It's a basic "smoke test" to ensure the wiring is correct.

        // Arrange
        Thought focusThought = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("In one sentence, what is the purpose of a cognitive architecture?", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMetadata(ThoughtType.GOAL, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );

        // Act
        List<Thought> resultThoughts = cognitiveProcessor.process(focusThought);

        // Assert
        assertNotNull(resultThoughts);
        assertFalse(resultThoughts.isEmpty());

        Thought result = resultThoughts.get(0);
        assertNotNull(result);
        assertEquals(ThoughtType.REPORT, result.metadata().type());
        assertNotNull(result.content().text());

        System.out.println("LLM Response: " + result.content().text());
    }
}
