package com.senars.cycle;

import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.llm.ToolKit;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action system that executes tool calls defined in an ACTION_PLAN
 * using a generic ToolExecutor.
 */
public class ToolUsingAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolUsingAction.class);
    private final ToolKit toolKit;
    private final ToolExecutor toolExecutor;

    public ToolUsingAction(ToolKit toolKit) {
        this.toolKit = toolKit;
        this.toolExecutor = new ToolExecutor(toolKit.getTools());
    }

    @Override
    public Feedback executePlan(Thought actionPlan) {
        long startTime = System.currentTimeMillis();

        if (actionPlan.content().symbolic() == null || actionPlan.content().symbolic().isBlank()) {
            LOGGER.warn("Attempted to execute a tool-using action plan with no symbolic content. ID: {}", actionPlan.id());
            return new Feedback(
                    ActionStatus.FAILURE,
                    "unknown",
                    "Action plan has no symbolic content.",
                    System.currentTimeMillis() - startTime,
                    actionPlan
            );
        }

        String toolRequestJson = actionPlan.content().symbolic();
        ToolExecutionRequest toolExecutionRequest = toolKit.parse(toolRequestJson);

        if (toolExecutionRequest == null) {
            String errorMsg = "Could not parse tool request from symbolic content: " + toolRequestJson;
            LOGGER.error(errorMsg);
            return new Feedback(
                    ActionStatus.FAILURE,
                    "unknown",
                    errorMsg,
                    System.currentTimeMillis() - startTime,
                    actionPlan
            );
        }

        try {
            String observation = toolExecutor.execute(toolExecutionRequest);
            long executionTime = System.currentTimeMillis() - startTime;
            LOGGER.info("Tool '{}' executed successfully in {}ms. Observation: {}", toolExecutionRequest.name(), executionTime, observation);

            return new Feedback(
                    ActionStatus.SUCCESS,
                    toolExecutionRequest.name(),
                    observation,
                    executionTime,
                    actionPlan
            );
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorMsg = "Error executing tool '" + toolExecutionRequest.name() + "': " + e.getMessage();
            LOGGER.error("Failed to execute tool request for action plan: {}", actionPlan.id(), e);

            return new Feedback(
                    ActionStatus.FAILURE,
                    toolExecutionRequest.name(),
                    errorMsg,
                    executionTime,
                    actionPlan
            );
        }
    }
}
