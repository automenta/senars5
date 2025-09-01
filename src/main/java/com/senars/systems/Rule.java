package com.senars.systems;

import com.senars.core.Thought;

import java.util.Optional;

/**
 * An interface for a single governance rule that can be applied to an action plan.
 */
@FunctionalInterface
public interface Rule {

    /**
     * Checks if a given action plan violates this rule.
     *
     * @param actionPlan The ACTION_PLAN Thought to check.
     * @return An Optional containing a reason for the veto if the rule is violated,
     *         or an empty Optional if the plan is compliant.
     */
    Optional<String> check(Thought actionPlan);
}
