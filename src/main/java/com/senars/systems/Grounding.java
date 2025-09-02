package com.senars.systems;

import com.senars.core.Feedback;

/**
 * Interface for the Grounding System, which is responsible for refining the system's
 * knowledge by processing feedback from executed actions.
 */
public interface Grounding {

    /**
     * Processes a raw Feedback object from an executed action to perform credit/blame
     * assignment. This involves adjusting the Clarity of Thoughts in the action's
     * provenance trace and potentially creating new goals to address failures.
     *
     * @param feedback A Feedback object containing the detailed outcome of an action.
     */
    void processFeedback(Feedback feedback);
}
