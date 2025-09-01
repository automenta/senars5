package com.senars.cycle;

import com.senars.core.Thought;

import java.util.List;

/**
 * Interface for the Perception System, which transforms raw multi-modal data
 * from the environment or user into new Thought objects.
 */
public interface Perception {

    /**
     * Processes raw input data and feedback from the action system, transforming
     * them into one or more perceptual Thoughts (e.g., BELIEF, QUESTION, REPORT types).
     *
     * @param feedbackQueue The queue containing feedback from executed actions.
     * @return A list of new Thoughts generated from all perceptual sources.
     */
    List<Thought> perceive(ActionFeedbackQueue feedbackQueue);
}
