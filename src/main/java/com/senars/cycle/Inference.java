package com.senars.cycle;

import com.senars.core.ThoughtType;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Inference {
    private static final Logger LOGGER = LoggerFactory.getLogger(Inference.class);
    private final Memory memory;
    private final LogicEngine logicEngine;

    public Inference(Memory memory, LogicEngine logicEngine) {
        this.memory = memory;
        this.logicEngine = logicEngine;
    }

    public boolean syncTheory() {
        LOGGER.debug("Syncing theory from Memory Nexus to Logic Engine...");
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
        if (theory.isBlank()) {
            LOGGER.warn("Theory from memory is blank. Clearing logic engine theory.");
            logicEngine.setTheory("");
            return false;
        }
        logicEngine.setTheory(theory);
        LOGGER.debug("Logic Engine theory synced.");
        return true;
    }

    public String executeQuery(String query) {
        LOGGER.info("Executing logical query: {}", query);

        // For now, we will perform a full sync before each query.
        // A more advanced implementation would sync based on events.
        boolean theoryExists = syncTheory();
        if (!theoryExists) {
            return "Error: The knowledge base is empty. No facts or rules are available.";
        }


        if (query == null || query.isBlank()) {
            return "Error: Query string cannot be empty.";
        }
        final String finalQuery = query.endsWith(".") ? query.substring(0, query.length() - 1) : query;

        // 4. Solve the query and format the solutions.
        List<Map<String, String>> solutions = logicEngine.solve(finalQuery);
        if (solutions.isEmpty()) {
            LOGGER.info("Query yielded no solutions.");
            return "Error: Query yielded no solutions.";
        }

        LOGGER.info("Found {} solutions for query.", solutions.size());
        return solutions.stream()
                .map(this::formatSolutionAsText)
                .collect(Collectors.joining("\n"));
    }

    private String formatSolutionAsText(Map<String, String> solution) {
        if (solution.isEmpty()) {
            return "Fact is true.";
        }
        return solution.entrySet().stream()
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
