package com.senars.cycle;

import com.senars.core.*;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles formal logical inference within the cognitive cycle.
 * This class uses a logic engine to derive new thoughts from existing beliefs and rules.
 */
public class Inference {

    private final Memory memory;

    public Inference(Memory memory) {
        this.memory = memory;
    }

    /**
     * Processes a logical query and returns the resulting thoughts.
     *
     * @param queryThought The thought containing the logical query (in its symbolic field).
     * @return A list of new BELIEF thoughts representing the solutions to the query.
     */
    public List<Thought> reason(Thought queryThought) {
        // 1. Gather all facts (from BELIEFS) and rules (from SCHEMAS) from memory.
        String facts = memory.getAllThoughts().stream()
                .filter(t -> t.metadata().type() == ThoughtType.BELIEF && t.content().symbolic() != null)
                .map(t -> t.content().symbolic() + ".") // Append dot for Prolog syntax
                .collect(Collectors.joining("\n"));

        String rules = memory.getAllThoughts().stream()
                .filter(t -> t.metadata().type() == ThoughtType.SCHEMA && t.content().rules() != null)
                .flatMap(t -> t.content().rules().stream())
                .collect(Collectors.joining("\n"));

        String theory = facts + "\n" + rules;

        // 2. Initialize the logic engine with the combined theory.
        LogicEngine logicEngine = new LogicEngine(theory);

        // 3. Get the query from the input thought.
        String queryString = queryThought.content().symbolic();
        if (queryString == null || queryString.isBlank()) {
            return List.of();
        }

        // 4. Solve the query and transform solutions into new BELIEF thoughts.
        List<Map<String, String>> solutions = logicEngine.solve(queryString);

        return solutions.stream()
                .map(solution -> createBeliefFromSolution(queryThought, queryString, solution))
                .collect(Collectors.toList());
    }

    /**
     * Creates a new BELIEF thought from a single solution to a query.
     */
    private Thought createBeliefFromSolution(Thought queryThought, String queryString, Map<String, String> solution) {
        String solvedQuery = applySolution(queryString, solution);
        String solutionText = formatSolutionAsText(solution);

        ThoughtContent newContent = new ThoughtContent(
                "Inferred: " + solutionText,
                solvedQuery,
                null, null, null, null, null
        );

        ThoughtState newState = new ThoughtState(1.0, 100.0, 1.0); // High clarity and salience

        ThoughtMeta newMeta = new ThoughtMeta(
                ThoughtType.BELIEF,
                ThoughtOrigin.LOGIC_INFERENCE,
                List.of(queryThought.id()), // Trace back to the query
                Instant.now()
        );

        return new Thought(UUID.randomUUID().toString(), newContent, newState, newMeta);
    }

    /**
     * Applies the solution substitution to the original query string to create a grounded fact.
     * This is a simple string replacement based on the variable names.
     */
    private String applySolution(String queryString, Map<String, String> solution) {
        String result = queryString;
        for (Map.Entry<String, String> entry : solution.entrySet()) {
            // Replace the variable (e.g., "Who") with its bound value.
            // Using regex to replace whole words only to avoid replacing substrings.
            result = result.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        return result;
    }

    /**
     * Formats a solution map into a human-readable string.
     */
    private String formatSolutionAsText(Map<String, String> solution) {
        return solution.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
