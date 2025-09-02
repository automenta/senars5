package com.senars.cycle;

import com.senars.core.*;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InferenceTest {

    @Mock
    private Memory memory;

    private Inference inference;

    @BeforeEach
    void setUp() {
        inference = new Inference(memory, new LogicEngine());
    }

    private Thought createBelief(String symbolic) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Belief: " + symbolic, symbolic, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );
    }

    private Thought createRuleSchema(List<String> rules) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Schema with rules", "rule_schema", null, null, null, null, rules),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );
    }

    private Thought createQueryGoal(String query) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Query: " + query, query, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
        );
    }

    @Test
    void executeQuery_withSimpleFactQuery_shouldReturnSuccess() {
        // Arrange
        Thought fact = createBelief("father(darth_vader, luke).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact));
        String query = "father(darth_vader, luke).";

        // Act
        String result = inference.executeQuery(query);

        // Assert
        assertEquals("Fact is true.", result);
    }

    @Test
    void executeQuery_withVariableQuery_shouldReturnBinding() {
        // Arrange
        Thought fact = createBelief("father(darth_vader, luke).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact));
        String query = "father(Who, luke).";

        // Act
        String result = inference.executeQuery(query);

        // Assert
        assertEquals("Who = darth_vader", result);
    }

    @Test
    void executeQuery_withRuleAndFact_shouldInferNewBelief() {
        // Arrange
        Thought fact1 = createBelief("father(darth_vader, luke).");
        Thought fact2 = createBelief("father(darth_vader, leia).");
        Thought rule = createRuleSchema(List.of("sibling(X, Y) :- father(Z, X), father(Z, Y), X \\== Y."));
        when(memory.getAllThoughts()).thenReturn(List.of(fact1, fact2, rule));
        String query = "sibling(luke, Who).";

        // Act
        String result = inference.executeQuery(query);

        // Assert
        assertEquals("Who = leia", result);
    }

    @Test
    void executeQuery_withMultipleSolutions_shouldReturnAll() {
        // Arrange
        Thought fact1 = createBelief("child(luke, darth_vader).");
        Thought fact2 = createBelief("child(leia, darth_vader).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact1, fact2));
        String query = "child(Who, darth_vader).";

        // Act
        String result = inference.executeQuery(query);

        // Assert
        assertTrue(result.contains("Who = luke"));
        assertTrue(result.contains("Who = leia"));
    }

    @Test
    void executeQuery_withNoMatchingFacts_shouldReturnError() {
        // Arrange
        Thought fact = createBelief("father(darth_vader, luke).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact));
        String query = "mother(Who, luke).";

        // Act
        String result = inference.executeQuery(query);

        // Assert
        assertEquals("Error: Query yielded no solutions.", result);
    }

    @Test
    void executeQuery_withEmptyTheory_shouldReturnError() {
        // Arrange
        when(memory.getAllThoughts()).thenReturn(Collections.emptyList());
        String query = "anything(X).";

        // Act
        String result = inference.executeQuery(query);

        // Assert
        assertEquals("Error: The knowledge base is empty. No facts or rules are available.", result);
    }
}
