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
        this.toolExecutor = new DefaultToolExecutor(this.tools);
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
            if (llmResponse != null && llmResponse.trim().startsWith("{")) {
                Map<String, Object> map = gson.fromJson(llmResponse, new com.google.gson.reflect.TypeToken<Map<String, Object>>() {
                }.getType());

                if (map.containsKey("name") && map.containsKey("arguments")) {
                    String name = (String) map.get("name");
                    Object argsObject = map.get("arguments");

                    String argumentsJson;
                    if (argsObject instanceof String) {
                        // If arguments is already a string, use it directly.
                        argumentsJson = (String) argsObject;
                    } else {
                        // Otherwise, serialize the map/object to a JSON string.
                        argumentsJson = gson.toJson(argsObject);
                    }

                    return ToolExecutionRequest.builder()
                            .name(name)
                            .arguments(argumentsJson)
                            .build();
                }
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse tool execution request from text: {}", llmResponse, e);
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
            return toolExecutor.execute(toolExecutionRequest, this);
        } catch (Exception e) {
            LOGGER.error("Error executing tool: {}", toolExecutionRequest.name(), e);
            return "Error: " + e.getMessage();
        }
    }
}
