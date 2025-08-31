package com.senars.llm;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMetadata;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    void testBuildWithPlaceholderImplementation() {
        // Arrange
        Thought focusThought = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("This is a test thought.", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );

        // Act
        String prompt = promptBuilder.build(null, focusThought, Collections.emptyList());

        // Assert
        String expectedPrompt = "Based on the following thought, what should be the next step? Thought: This is a test thought.";
        assertEquals(expectedPrompt, prompt);
    }

    @Test
    void testBuildWithSchemaAndContext() {
        // Arrange
        Thought schema = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Analyze the following. Main subject: {{focus}}. Supporting data: {{context}}.", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMetadata(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );

        Thought focusThought = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("The topic is AI.", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMetadata(ThoughtType.GOAL, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );

        List<Thought> context = List.of(
                new Thought(
                        UUID.randomUUID().toString(),
                        new ThoughtContent("AI is advancing quickly.", null, null, null, null, null),
                        new ThoughtState(1.0, 1.0, 1.0),
                        new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.PERCEPTION, Collections.emptyList(), Instant.now())
                ),
                new Thought(
                        UUID.randomUUID().toString(),
                        new ThoughtContent("There are many new models.", null, null, null, null, null),
                        new ThoughtState(1.0, 1.0, 1.0),
                        new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.PERCEPTION, Collections.emptyList(), Instant.now())
                )
        );

        // Act
        String prompt = promptBuilder.build(schema, focusThought, context);

        // Assert
        String expectedPrompt = "Analyze the following. Main subject: The topic is AI.. Supporting data: - AI is advancing quickly.\n- There are many new models.\n.";
        assertEquals(expectedPrompt, prompt);
    }
}
