package com.senars.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Represents the contextual information and lineage of a Thought.
 *
 * @param type        The cognitive type of the Thought.
 * @param origin      The source of the Thought's creation.
 * @param trace       An ordered list of Thought IDs that led to this Thought's creation (deprecated, for backward compatibility).
 * @param causalLinks A set of causal links that led to this Thought's creation (new graph-based approach).
 * @param timestamp   The creation time for recency and decay calculations.
 */
public record ThoughtMeta(
        ThoughtType type,
        ThoughtOrigin origin,
        List<String> trace,
        Set<CausalLink> causalLinks,
        Instant timestamp
) implements Serializable {

    /**
     * Constructor for backward compatibility that creates an empty causalLinks set
     */
    public ThoughtMeta(ThoughtType type, ThoughtOrigin origin, List<String> trace, Instant timestamp) {
        this(type, origin, trace, Set.of(), timestamp);
    }

    /**
     * Constructor with causalLinks
     */
    public ThoughtMeta {
        // Ensure trace is never null
        if (trace == null) {
            trace = List.of();
        }

        // Ensure causalLinks is never null
        if (causalLinks == null) {
            causalLinks = Set.of();
        }
    }
}
