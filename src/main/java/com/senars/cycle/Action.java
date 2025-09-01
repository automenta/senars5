package com.senars.cycle;

import com.senars.core.Thought;

/**
 * Interface for the Action System, which is responsible for translating an
 * approved ACTION_PLAN Thought into operations in the external world.
 */
public interface Action {

    /**
     * Executes the given action plan. This method is called only after the
     * plan has been approved by the Governance Layer. The execution may
     * result in effects on the external environment (e.g., API calls,
     * robotic commands, sending messages). The results of the action
     * should be placed into the feedback queue as new REPORT thoughts.
     *
     * @param actionPlan The approved ACTION_PLAN Thought to execute.
     * @param feedbackQueue The queue to place the resulting REPORT thought into.
     */
    void executePlan(Thought actionPlan, ActionFeedbackQueue feedbackQueue);
}
