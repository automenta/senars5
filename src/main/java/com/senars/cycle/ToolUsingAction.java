package com.senars.cycle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.senars.core.*;
import com.senars.llm.ToolKit;
import com.senars.tools.SearchTools;
import com.senars.tools.WebTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An action system that executes tool calls defined in an ACTION_PLAN.
 */
public class ToolUsingAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolUsingAction.class);
    private final SearchTools searchTools;
    private final WebTools webTools;
    private final Gson gson = new Gson();

    public ToolUsingAction(ToolKit toolKit) {
        // This is a simplified dispatcher. A more robust solution might use a map of tool names to objects.
        this.searchTools = (SearchTools) toolKit.getTools().stream().filter(t -> t instanceof SearchTools).findFirst().orElseThrow();
        this.webTools = (WebTools) toolKit.getTools().stream().filter(t -> t instanceof WebTools).findFirst().orElseThrow();
    }

    @Override
    public void executePlan(Thought actionPlan, ActionFeedbackQueue feedbackQueue) {
        if (actionPlan.content().symbolic() == null || actionPlan.content().symbolic().isBlank()) {
            LOGGER.warn("Attempted to execute a tool-using action plan with no symbolic content. ID: {}", actionPlan.id());
            return;
        }

        try {
            // Manual parsing and dispatching, as a stub for a more robust ToolExecutor
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> toolRequestMap = gson.fromJson(actionPlan.content().symbolic(), type);

            String toolName = (String) toolRequestMap.get("name");
            Map<String, Object> arguments = (Map<String, Object>) toolRequestMap.get("arguments");
            String observation;

            if ("search".equals(toolName)) {
                String query = (String) arguments.get("query");
                observation = searchTools.search(query);
            } else if ("read".equals(toolName)) {
                String url = (String) arguments.get("url");
                observation = webTools.read(url);
            } else {
                throw new UnsupportedOperationException("Tool not found: " + toolName);
            }

            LOGGER.info("Tool {} executed successfully. Observation: {}", toolName, observation);
            feedbackQueue.add(createReport(observation, actionPlan.id()));

        } catch (Exception e) {
            LOGGER.error("Failed to execute tool request for action plan: {}", actionPlan.id(), e);
            String errorMessage = "Error executing tool: " + e.getMessage();
            feedbackQueue.add(createReport(errorMessage, actionPlan.id()));
        }
    }

    private Thought createReport(String text, String originatingPlanId) {
        ThoughtContent content = new ThoughtContent(text, null, null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.REPORT,
                ThoughtOrigin.SYSTEM, // The system's action created this report
                List.of(originatingPlanId),
                Instant.now()
        );
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0); // Reports are high clarity
        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
