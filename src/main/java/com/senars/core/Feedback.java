package com.senars.core;

/**
 * A structured representation of feedback on an action's outcome.
 * This is intended to be part of the content of a REPORT Thought.
 *
 * @param success A metric indicating the degree of success, typically in the range [0.0, 1.0].
 * @param correction An optional, descriptive correction or explanation for failures.
 */
public record Feedback(
    double success,
    String correction
) {}
