package com.senars.systems;

import com.senars.core.Thought;

/**
 * Interface for the Grounding System, which is responsible for refining the system's
 * knowledge by processing feedback from executed actions.
 */
public interface Grounding {

    /**
     * Processes a feedback report (typically a REPORT Thought) to perform credit/blame
     * assignment, adjusting the Clarity of Thoughts in the action's provenance trace.
     *
     * @param feedbackReport A Thought (usually of type REPORT) containing details about the
     *                       outcome of an action.
     */
    void processFeedback(Thought feedbackReport);
}
