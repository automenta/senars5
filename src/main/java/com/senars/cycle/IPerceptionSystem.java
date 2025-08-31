package com.senars.cycle;

import com.senars.core.Thought;
import java.util.List;

/**
 * Interface for the Perception System, which transforms raw multi-modal data
 * from the environment or user into new Thought objects.
 */
public interface IPerceptionSystem {

    /**
     * Processes some form of raw input data and transforms it into one or more
     * perceptual Thoughts (e.g., BELIEF or QUESTION types).
     *
     * @param rawInput The raw input data (e.g., text, image data, sensor readings).
     * @return A list of new Thoughts generated from the input.
     */
    List<Thought> perceive(Object rawInput);
}
