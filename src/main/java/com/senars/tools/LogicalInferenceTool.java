package com.senars.tools;

import com.senars.cycle.Inference;
import com.senars.systems.Memory;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalInferenceTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogicalInferenceTool.class);
    private final Inference inference;

    public LogicalInferenceTool(Memory memory) {
        this.inference = new Inference(memory);
    }

    @Tool("Executes a formal logical query against the system's knowledge base of beliefs and rules. The query must be in Prolog format.")
    public String executeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            LOGGER.warn("Query string is null or empty.");
            return "Error: Query string cannot be empty.";
        }
        LOGGER.info("Executing logical query: {}", query);
        try {
            return inference.executeQuery(query);
        } catch (Exception e) {
            LOGGER.error("Error executing logical query", e);
            return "Error: " + e.getMessage();
        }
    }
}
