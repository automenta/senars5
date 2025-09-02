package com.senars.cycle;

import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.lm.ToolKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

/**
 * An action system that executes tool calls defined in an ACTION.
 */
public class ToolUsingAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolUsingAction.class);
    private final ToolKit toolKit;

    public ToolUsingAction(ToolKit toolKit) {
        this.toolKit = toolKit;
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
        ToolExecutionRequest toolRequest = toolKit.parse(toolRequestJson);

        if (toolRequest == null) {
            LOGGER.error("Failed to parse tool request from symbolic content: {}", toolRequestJson);
            return new Feedback(
                    ActionStatus.FAILURE,
                    "parser",
                    "Could not parse tool request JSON.",
                    System.currentTimeMillis() - startTime,
                    actionPlan
            );
        }

        String observation = toolKit.execute(toolRequest);
        long executionTime = System.currentTimeMillis() - startTime;

        // Simple heuristic: if the observation starts with "Error:", treat it as a failure.
        ActionStatus status = observation.startsWith("Error:") ? ActionStatus.FAILURE : ActionStatus.SUCCESS;

        LOGGER.info("Tool {} execution finished with status {} in {}ms. Observation: {}", toolRequest.name(), status, executionTime, observation);

        return new Feedback(
                status,
                toolRequest.name(),
                observation,
                executionTime,
                actionPlan
        );
    }
}
