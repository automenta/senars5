package com.senars.effort;

import com.senars.core.Thought;

import java.io.Serializable;

/**
 * A simple effort prediction model that calculates effort as a linear function
 * of the length of the Thought's text content.
 *
 * The formula is: effort = (coefficient * textLength) + intercept.
 *
 * @param coefficient The multiplier for the text length.
 * @param intercept   A constant value added to the result.
 */
public record LinearTextEffortModel(double coefficient,
                                    double intercept) implements IEffortPredictionModel, Serializable {

    /**
     * A default model to use when no specific model is configured.
     */
    public static final LinearTextEffortModel DEFAULT = new LinearTextEffortModel(0.1, 1.0);

    @Override
    public double predict(Thought thought) {
        if (thought == null || thought.content() == null || thought.content().text() == null) {
            return intercept; // Return the base effort if there's no text.
        }

        int textLength = thought.content().text().length();
        double effort = (coefficient * textLength) + intercept;

        // Ensure effort is never zero or negative, as it's used as a divisor.
        return Math.max(0.0001, effort);
    }
}
