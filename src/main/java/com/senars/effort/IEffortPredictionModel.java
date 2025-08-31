package com.senars.effort;

import com.senars.core.Thought;

/**
 * An interface for all effort prediction models.
 * An effort prediction model is a component of a SCHEMA thought that estimates
 * the computational cost to process a given Thought.
 */
public interface IEffortPredictionModel {

    /**
     * Predicts the effort required to process a given Thought.
     *
     * @param thought The Thought for which to predict the effort.
     * @return A double representing the predicted effort.
     */
    double predict(Thought thought);
}
