package com.senars.cycle;

import com.senars.core.*;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles formal logical inference within the cognitive cycle.
 * This class uses a logic engine to derive new thoughts from existing beliefs and rules.
 */
public class Inference {

    private static final Logger LOGGER = LoggerFactory.getLogger(Inference.class);
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
                .filter(t -> t.metadata().type() == ThoughtType.BELIEF && t.content().symbolic() != null && !t.content().symbolic().isBlank())
                .map(t -> t.content().symbolic().endsWith(".") ? t.content().symbolic() : t.content().symbolic() + ".")
                .collect(Collectors.joining("\n"));

        String rules = memory.getAllThoughts().stream()
                .filter(t -> t.metadata().type() == ThoughtType.SCHEMA && t.content().rules() != null)
                .flatMap(t -> t.content().rules().stream())
                .map(rule -> rule.endsWith(".") ? rule : rule + ".")
                .collect(Collectors.joining("\n"));

        String theory = facts + "\n" + rules;
        LOGGER.debug("Constructed Prolog Theory:\n{}", theory);

        if (theory.isBlank()) {
            LOGGER.warn("Cannot perform inference with an empty theory.");
            return Collections.emptyList();
        }

        // 2. Initialize the logic engine with the combined theory.
        LogicEngine logicEngine = new LogicEngine(theory);

        // 3. Get the query from the input thought.
        String originalQueryString = queryThought.content().symbolic();
        if (originalQueryString == null || originalQueryString.isBlank()) {
            LOGGER.warn("Query thought {} has no symbolic query.", queryThought.id());
            return Collections.emptyList();
        }

        // Queries in tuProlog don't end with a dot. This variable must be final to be used in the lambda.
        final String finalQueryString = originalQueryString.endsWith(".") ? originalQueryString.substring(0, originalQueryString.length() - 1) : originalQueryString;

        LOGGER.info("Executing logical query: {}", finalQueryString);

        // 4. Solve the query and transform solutions into new BELIEF thoughts.
        List<Map<String, String>> solutions = logicEngine.solve(finalQueryString);
        LOGGER.info("Found {} solutions for query.", solutions.size());

        return solutions.stream()
                .map(solution -> createBeliefFromSolution(queryThought, finalQueryString, solution))
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
                null,   // embedding
                null,   // perceptual
                null,   // procedural
                null,   // feedback
                null    // rules
        );

        // New thoughts start with high clarity and salience, to be adjusted by the system later.
        ThoughtState newState = new ThoughtState(1.0, 1.0, 1.0);

        ThoughtMeta newMeta = new ThoughtMeta(
                ThoughtType.BELIEF,
                ThoughtOrigin.LOGIC_INFERENCE,
                List.of(queryThought.id()), // Trace back to the query thought
                Instant.now()
        );

        Thought newThought = new Thought(UUID.randomUUID().toString(), newContent, newState, newMeta);
        LOGGER.debug("Created new BELIEF thought {} from solution: {}", newThought.id(), solvedQuery);
        return newThought;
    }

    /**
     * Applies the solution substitution to the original query string to create a grounded fact.
     * e.g., query "father(X, 'luke')" with solution {X=darth_vader} becomes "father('darth_vader', 'luke')"
     */
    private String applySolution(String queryString, Map<String, String> solution) {
        String result = queryString;
        for (Map.Entry<String, String> entry : solution.entrySet()) {
            // Replace the variable (e.g., "X") with its bound value.
            result = result.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        return result;
    }

    /**
     * Formats a solution map into a human-readable string.
     * e.g., {X=darth_vader, Y=leia} becomes "X = 'darth_vader', Y = 'leia'"
     */
    private String formatSolutionAsText(Map<String, String> solution) {
        if (solution.isEmpty()) {
            return "Fact is true.";
        }
        return solution.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
