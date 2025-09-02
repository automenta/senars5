package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.cycle.Action;
import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stub implementation of the action system that simply logs the action plan.
 * It's useful for wiring the application when no real action execution is needed.
 */
public class StubAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(StubAction.class);

    @Override
    public Feedback executePlan(Thought actionPlan) {
        long startTime = System.currentTimeMillis();
        String planText = actionPlan.content().text() != null ? actionPlan.content().text() : "No textual content";
        LOGGER.info("Stub executing action plan for thought {}: {}", actionPlan.id(), planText);

        // This stub does not perform any real-world actions.
        // It returns a success feedback to allow the cognitive cycle to continue.
        String observation = "Action plan logged by StubAction.";
        long executionTime = System.currentTimeMillis() - startTime;
        return new Feedback(ActionStatus.SUCCESS, "stub.log", observation, executionTime, actionPlan);
    }
}
