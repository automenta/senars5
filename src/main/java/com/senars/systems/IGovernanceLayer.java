package com.senars.systems;

import com.senars.core.Thought;
import java.util.Optional;

/**
 * Interface for the Governance Layer, the immutable safety backstop for the system.
 * It is responsible for vetoing unsafe or unethical actions.
 */
public interface IGovernanceLayer {

    /**
     * Reviews an ACTION_PLAN Thought to determine if it complies with the system's
     * safety and ethical guidelines.
     *
     * @param actionPlan A Thought of type ACTION_PLAN.
     * @return An Optional containing a reason for the veto if the plan is rejected,
     *         or an empty Optional if the plan is approved.
     */
    Optional<String> reviewPlan(Thought actionPlan);

}
