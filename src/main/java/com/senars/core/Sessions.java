package com.senars.core;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the session context for the cognitive cycle.
 * This class is responsible for tracking transient information that needs to
 * persist across individual cognitive steps, such as the last action taken.
 * It is designed to be thread-safe.
 */
public class Sessions {

    private final AtomicReference<Thought> lastActionPlan = new AtomicReference<>();

    /**
     * Retrieves the last action plan that was executed.
     * This is useful for linking feedback to the action that prompted it.
     *
     * @return An Optional containing the last action plan, or empty if none has been set.
     */
    public Optional<Thought> getLastActionPlan() {
        return Optional.ofNullable(this.lastActionPlan.get());
    }

    /**
     * Sets the last executed action plan.
     * This is typically called by the cognitive cycle after an action plan has been
     * approved and is about to be executed.
     *
     * @param actionPlan The ACTION_PLAN thought that was executed.
     */
    public void setLastActionPlan(Thought actionPlan) {
        if (actionPlan != null && actionPlan.metadata().type() != ThoughtType.ACTION_PLAN) {
            throw new IllegalArgumentException("Only ACTION_PLAN thoughts can be set as the last action plan.");
        }
        this.lastActionPlan.set(actionPlan);
    }

    /**
     * Clears the last action plan.
     * This can be used after feedback has been processed to prevent reusing the same action plan.
     */
    public void clearLastActionPlan() {
        this.lastActionPlan.set(null);
    }
}
