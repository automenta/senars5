package com.senars.systems.vectorstore;

/**
 * A container class that pairs a Thought ID with a relevance score.
 *
 * @param id The ID of the thought.
 * @param score   The similarity or relevance score.
 */
public record ScoredId(String id, double score) {
}
