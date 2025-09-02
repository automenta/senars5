package com.senars.core;

import java.io.Serializable;

/**
 * Represents the dynamic state of a Thought, which evolves over time.
 * Using a record implies that state evolution happens by creating new instances
 * of Thoughts with new states, rather than by mutation.
 *
 * @param clarity    [0, 1] System's confidence/truth value in this Thought.
 * @param salience   [0, ∞) Current attentional priority, dynamically calculated.
 * @param activation [0, 1] How "close to the surface" this Thought is in Memory Nexus.
 */
public record ThoughtState(
        double clarity,
        double salience,
        double activation
) implements Serializable {
}
