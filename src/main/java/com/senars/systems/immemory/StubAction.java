package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.cycle.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stub implementation of the action system that simply logs the action plan.
 * It's useful for wiring the application when no real action execution is needed.
 */
public class StubAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(StubAction.class);

    @Override
    public void executePlan(Thought actionPlan) {
        LOGGER.info("Executing action plan for thought {}: {}", actionPlan.id(), actionPlan.content().text());
        // This stub does not perform any real-world actions.
    }
}
