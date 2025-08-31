package com.senars.cycle;

import com.senars.core.Thought;

/**
 * Interface for the Action System, which is responsible for translating an
 * approved ACTION_PLAN Thought into operations in the external world.
 */
public interface IActionSystem {

    /**
     * Executes the given action plan. This method is called only after the
     * plan has been approved by the Governance Layer. The execution may
     * result in effects on the external environment (e.g., API calls,
     * robotic commands, sending messages).
     *
     * @param actionPlan The approved ACTION_PLAN Thought to execute.
     */
    void executePlan(Thought actionPlan);
}
