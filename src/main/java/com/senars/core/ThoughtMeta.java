package com.senars.core;

import java.time.Instant;
import java.util.List;

/**
 * Represents the contextual information and lineage of a Thought.
 *
 * @param type      The cognitive type of the Thought.
 * @param origin    The source of the Thought's creation.
 * @param trace     An ordered list of Thought IDs that led to this Thought's creation.
 * @param timestamp The creation time for recency and decay calculations.
 */
public record ThoughtMeta(
        ThoughtType type,
        ThoughtOrigin origin,
        List<String> trace,
        Instant timestamp
) {
}
