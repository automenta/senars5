package com.senars.llm;

import com.senars.core.*;
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
                new ThoughtContent("This is a test thought.", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );

        // Act
        String prompt = promptBuilder.build(null, focusThought, Collections.emptyList(), Collections.emptyList());

        // Assert
        String expectedPrompt = "You are a helpful reasoning engine. Your goal is to decide the next best step.\n\n" +
                "The current focus is a BELIEF with the content: 'This is a test thought.'.\n" +
                "What is the next logical step or action?";
        assertEquals(expectedPrompt, prompt);
    }

    @Test
    void testBuildWithSchemaAndContext() {
        // Arrange
        Thought schema = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Analyze the following. Main subject: {{focus}}. Supporting data: {{context}}.", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );

        Thought focusThought = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("The topic is AI.", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );

        List<Thought> context = List.of(
                new Thought(
                        UUID.randomUUID().toString(),
                        new ThoughtContent("AI is advancing quickly.", null, null, null, null, null, null),
                        new ThoughtState(1.0, 1.0, 1.0),
                        new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.PERCEPTION, Collections.emptyList(), Instant.now())
                ),
                new Thought(
                        UUID.randomUUID().toString(),
                        new ThoughtContent("There are many new models.", null, null, null, null, null, null),
                        new ThoughtState(1.0, 1.0, 1.0),
                        new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.PERCEPTION, Collections.emptyList(), Instant.now())
                )
        );

        // Act
        String prompt = promptBuilder.build(schema, focusThought, context, Collections.emptyList());

        // Assert
        String expectedPrompt = "Analyze the following. Main subject: {{focus}}. Supporting data: {{context}}.\n\n" +
                "Here is some context from previous thoughts:\n" +
                "- [BELIEF] AI is advancing quickly.\n" +
                "- [BELIEF] There are many new models.\n\n" +
                "The current focus is a GOAL with the content: 'The topic is AI.'.\n" +
                "What is the next logical step or action?";
        assertEquals(expectedPrompt, prompt);
    }
}
