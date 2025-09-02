package com.senars.cycle;

import com.senars.core.Thought;

import java.util.List;

/**
 * Interface for the Perception System, which transforms raw multi-modal data
 * from the environment or user into new Thought objects.
 */
public interface Perception {

    /**
     * Processes raw input data from the environment (e.g., user input, sensors)
     * and transforms it into one or more perceptual Thoughts (e.g., BELIEF, QUESTION).
     *
     * @return A list of new Thoughts generated from external perceptual sources.
     * @throws ShutdownException if a shutdown command is detected from a perception channel.
     */
    List<Thought> perceive() throws ShutdownException;
}
