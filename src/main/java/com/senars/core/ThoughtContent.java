package com.senars.core;

import java.util.List;

/**
 * Represents the multi-modal content of a Thought.
 * This record is immutable. All fields can be null to indicate that a
 * particular content type is not present for this Thought.
 *
 * @param text       Primary natural language representation.
 * @param symbolic   Formal logic or symbolic representation (e.g., a schema name).
 * @param embedding  Vector embedding for semantic search.
 * @param perceptual Raw or processed sensory data (e.g., image tensor).
 * @param procedural Definition of a procedure, like a prompt chain, code, or a model object.
 * @param feedback   Structured feedback on an action's outcome, typically for REPORT thoughts.
 */
public record ThoughtContent(
    String text,
    String symbolic,
    List<Double> embedding,
    Object perceptual,
    Object procedural,
    Feedback feedback
) {}
