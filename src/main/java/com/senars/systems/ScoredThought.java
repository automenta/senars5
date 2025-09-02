package com.senars.systems;

import com.senars.core.Thought;

/**
 * A container class that pairs a Thought with a relevance score,
 * typically from a vector search.
 *
 * @param thought The retrieved thought.
 * @param score   The similarity or relevance score (e.g., cosine similarity).
 */
public record ScoredThought(Thought thought, double score) {
}
