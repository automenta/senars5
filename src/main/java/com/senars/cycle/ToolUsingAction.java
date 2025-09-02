package com.senars.cycle;

import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.llm.ToolKit;
import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.llm.ToolKit;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action system that executes tool calls defined in an ACTION_PLAN.
 * NOTE: This is a temporary stub implementation to get the system to compile.
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
        LOGGER.info("Received tool request: {}", toolRequestJson);

        // TODO: This is a stub. A proper implementation needs to be created.
        String observation = "Tool execution is not yet implemented.";
        long executionTime = System.currentTimeMillis() - startTime;
        return new Feedback(
                ActionStatus.SUCCESS,
                "stub.tool",
                observation,
                executionTime,
                actionPlan
        );
    }
}
