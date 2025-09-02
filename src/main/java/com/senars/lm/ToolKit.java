package com.senars.lm;

import com.google.gson.Gson;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Manages the set of available tools for the SeNARS agent.
 * It provides utilities to get tool specifications for LLM prompts and to parse LLM responses
 * into tool execution requests.
 */
public class ToolKit {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolKit.class);
    private final List<Object> tools;
    private final ToolExecutor toolExecutor;
    private final Gson gson = new Gson();

    public ToolKit(Object... tools) {
        this.tools = Arrays.asList(tools);
        this.toolExecutor = new ToolExecutor(this.tools);
    }

    /**
     * Gets the specifications of all available tools.
     * @return A list of ToolSpecification objects.
     */
    public List<ToolSpecification> getToolSpecifications() {
        return ToolSpecifications.toolSpecificationsFrom(tools);
    }

    /**
     * Parses an LLM response to see if it contains a valid tool execution request.
     * @param llmResponse The response string from the LLM.
     * @return A ToolExecutionRequest if the response is a valid tool call, otherwise null.
     */
    public ToolExecutionRequest parse(String llmResponse) {
        try {
            // This is a simplified, stubbed parser. A more robust implementation would be needed for production.
            if (llmResponse.trim().startsWith("{") && llmResponse.contains("\"name\"") && llmResponse.contains("\"arguments\"")) {
                Map<String, Object> map = gson.fromJson(llmResponse, Map.class);
                String name = (String) map.get("name");
                Map<String, Object> arguments = (Map<String, Object>) map.get("arguments");
                // Note: The ID of the request is not available in this simplified parsing.
                return ToolExecutionRequest.builder()
                        .name(name)
                        .arguments(gson.toJson(arguments))
                        .build();
            }
            return null;
        } catch (Exception e) {
            // The response was not a valid tool execution request JSON
            return null;
        }
    }

    /**
     * Executes a tool request.
     * @param toolExecutionRequest The request to execute.
     * @return The result of the tool execution as a String.
     */
    public String execute(ToolExecutionRequest toolExecutionRequest) {
        LOGGER.info("Executing tool: {}", toolExecutionRequest.name());
        try {
            return toolExecutor.execute(toolExecutionRequest);
        } catch (Exception e) {
            LOGGER.error("Error executing tool: {}", toolExecutionRequest.name(), e);
            return "Error: " + e.getMessage();
        }
    }
}
