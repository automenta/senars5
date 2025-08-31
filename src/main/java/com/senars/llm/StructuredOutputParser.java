package com.senars.llm;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMetadata;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senars.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Parses the structured output (e.g., JSON) from the LLM into a list of Thought objects.
 */
public class StructuredOutputParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredOutputParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses the LLM's response string into a list of Thoughts.
     *
     * @param llmResponse The response from the language model, expected to be in a structured format.
     * @return A list of new Thought objects.
     */
    public List<Thought> parse(String llmResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(llmResponse);

            ThoughtType type = ThoughtType.valueOf(rootNode.get("type").asText("BELIEF"));
            String content = rootNode.get("content").asText();
            double clarity = rootNode.get("clarity").asDouble(0.8); // Default clarity
            double salience = rootNode.get("salience").asDouble(50.0); // Default salience

            Thought thought = new Thought(
                    UUID.randomUUID().toString(),
                    new ThoughtContent(content, null, null, null, null),
                    new ThoughtState(clarity, salience, 1.0), // Default activation
                    new ThoughtMetadata(
                            type,
                            ThoughtOrigin.LLM_INFERENCE,
                            Collections.emptyList(),
                            Instant.now()
                    )
            );
            return List.of(thought);

        } catch (IOException e) {
            LOGGER.warn("Failed to parse LLM response as JSON. Falling back to simple report. Error: {}", e.getMessage());
            return List.of(createReportThought(llmResponse));
        }
    }

    private Thought createReportThought(String content) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(content, null, null, null, null),
                new ThoughtState(0.9, 50.0, 1.0), // High clarity, medium salience, high activation
                new ThoughtMetadata(
                        ThoughtType.REPORT,
                        ThoughtOrigin.LLM_INFERENCE,
                        Collections.emptyList(),
                        Instant.now()
                )
        );
    }
}
