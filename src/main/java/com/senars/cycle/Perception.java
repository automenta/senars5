package com.senars.cycle;

import com.senars.core.Thought;

import java.util.List;

/**
 * Interface for the Perception System, which transforms raw multi-modal data
 * from the environment or user into new Thought objects.
 */
public interface Perception {

    /**
     * Processes some form of raw input data and transforms it into one or more
     * perceptual Thoughts (e.g., BELIEF or QUESTION types).
     *
     * @return A list of new Thoughts generated from the input.
     */
    List<Thought> perceive();
}
