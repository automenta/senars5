package com.senars.cycle;

import com.senars.core.Feedback;
import com.senars.core.Thought;

/**
 * Interface for the Action System, which is responsible for translating an
 * approved ACTION Thought into operations in the external world.
 */
public interface Action {

    /**
     * Executes the given action plan. This method is called only after the
     * plan has been approved by the Governance Layer. The execution may
     * result in effects on the external environment (e.g., API calls,
     * robotic commands, sending messages). The results of the action
     * are returned in a structured Feedback object for the Grounding system
     * to process.
     *
     * @param actionPlan The approved ACTION Thought to execute.
     * @return A Feedback object containing the outcome of the action.
     */
    Feedback executePlan(Thought actionPlan);
}
