package com.senars.cycle;

import com.senars.core.*;
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
        inference = new Inference(memory);
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
    void reason_withSimpleFactQuery_shouldReturnOneBelief() {
        // Arrange
        Thought fact = createBelief("father(darth_vader, luke).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact));
        Thought query = createQueryGoal("father(darth_vader, luke).");

        // Act
        List<Thought> results = inference.reason(query);

        // Assert
        assertEquals(1, results.size());
        Thought resultThought = results.getFirst();
        assertEquals(ThoughtType.BELIEF, resultThought.metadata().type());
        assertEquals("father(darth_vader, luke)", resultThought.content().symbolic());
        assertTrue(resultThought.content().text().contains("Fact is true"));
    }

    @Test
    void reason_withVariableQuery_shouldReturnCorrectBelief() {
        // Arrange
        Thought fact = createBelief("father(darth_vader, luke).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact));
        Thought query = createQueryGoal("father(Who, luke).");

        // Act
        List<Thought> results = inference.reason(query);

        // Assert
        assertEquals(1, results.size());
        Thought resultThought = results.getFirst();
        assertEquals(ThoughtType.BELIEF, resultThought.metadata().type());
        assertEquals("father(darth_vader, luke)", resultThought.content().symbolic());
        assertEquals("Inferred: Who = darth_vader", resultThought.content().text());
    }

    @Test
    void reason_withRuleAndFact_shouldInferNewBelief() {
        // Arrange
        Thought fact1 = createBelief("father(darth_vader, luke).");
        Thought fact2 = createBelief("father(darth_vader, leia).");
        Thought rule = createRuleSchema(List.of("sibling(X, Y) :- father(Z, X), father(Z, Y), X \\== Y."));
        when(memory.getAllThoughts()).thenReturn(List.of(fact1, fact2, rule));
        Thought query = createQueryGoal("sibling(luke, Who).");

        // Act
        List<Thought> results = inference.reason(query);

        // Assert
        assertEquals(1, results.size());
        Thought resultThought = results.getFirst();
        assertEquals("sibling(luke, leia)", resultThought.content().symbolic());
        assertEquals("Inferred: Who = leia", resultThought.content().text());
    }

    @Test
    void reason_withQueryReturningMultipleSolutions_shouldReturnMultipleBeliefs() {
        // Arrange
        Thought fact1 = createBelief("child(luke, darth_vader).");
        Thought fact2 = createBelief("child(leia, darth_vader).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact1, fact2));
        Thought query = createQueryGoal("child(Who, darth_vader).");

        // Act
        List<Thought> results = inference.reason(query);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(t -> t.content().symbolic().equals("child(luke, darth_vader)")));
        assertTrue(results.stream().anyMatch(t -> t.content().symbolic().equals("child(leia, darth_vader)")));
    }

    @Test
    void reason_withNoMatchingFacts_shouldReturnEmptyList() {
        // Arrange
        Thought fact = createBelief("father(darth_vader, luke).");
        when(memory.getAllThoughts()).thenReturn(List.of(fact));
        Thought query = createQueryGoal("mother(Who, luke).");

        // Act
        List<Thought> results = inference.reason(query);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void reason_withEmptyTheory_shouldReturnEmptyList() {
        // Arrange
        when(memory.getAllThoughts()).thenReturn(Collections.emptyList());
        Thought query = createQueryGoal("anything(X).");

        // Act
        List<Thought> results = inference.reason(query);

        // Assert
        assertTrue(results.isEmpty());
    }
}
