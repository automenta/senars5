package com.senars.lm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.senars.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Parses the structured output (a JSON array of Thought objects) from the LLM into a list of Thought objects.
 */
public class StructuredOutputParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredOutputParser.class);
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new parser and configures the ObjectMapper.
     */
    public StructuredOutputParser() {
        this.objectMapper = new ObjectMapper();
        // Register the module that handles Java 8 date/time types like Instant
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Parses the LLM's response string, expecting a JSON array of Thought objects.
     *
     * @param llmResponse The response from the language model.
     * @return A list of new Thought objects. Returns an empty list if parsing fails.
     */
    public List<Thought> parse(String llmResponse) {
        try {
            // We expect the LLM to return a JSON array of Thought objects.
            // Using TypeReference allows Jackson to correctly deserialize the generic List<Thought>.
            return objectMapper.readValue(llmResponse, new TypeReference<>() {
            });
        } catch (IOException e) {
            LOGGER.warn("Failed to parse LLM response as JSON array. Falling back to simple report. Error: {}", e.getMessage());
            // Fallback for non-JSON or malformed responses
            return List.of(createReportThought(llmResponse));
        }
    }

    private Thought createReportThought(String content) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(content, null, null, null, null, null, null),
                new ThoughtState(0.9, 50.0, 1.0), // High clarity, medium salience, high activation
                new ThoughtMeta(
                        ThoughtType.REPORT,
                        ThoughtOrigin.LLM_INFERENCE,
                        Collections.emptyList(),
                        Instant.now()
                )
        );
    }
}
