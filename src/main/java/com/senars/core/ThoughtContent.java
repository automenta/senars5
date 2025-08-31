package com.senars.core;

import java.util.List;

/**
 * Represents the multi-modal content of a Thought.
 * Fields can be null to indicate that a particular content type is not present.
 *
 * @param text       Primary natural language representation.
 * @param symbolic   Formal logic or symbolic representation.
 * @param embedding  Vector embedding for semantic search.
 * @param perceptual Raw or processed sensory data.
 * @param procedural Definition of a procedure, like a prompt chain or code.
 */
public record ThoughtContent(
    String text,
    String symbolic,
    List<Double> embedding,
    Object perceptual,
    Object procedural
) {}
