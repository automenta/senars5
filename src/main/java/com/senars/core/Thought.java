package com.senars.core;

import java.io.Serializable;

/**
 * The universal data structure for all cognitive phenomena in the SeNARS system.
 * It is an immutable record, meaning any "modification" results in a new Thought instance.
 *
 * @param id       A unique identifier for provenance tracking.
 * @param content  The multi-modal content of the Thought.
 * @param state    The dynamic, evolving state of the Thought.
 * @param metadata The contextual information and lineage of the Thought.
 */
public record Thought(
        String id,
        ThoughtContent content,
        ThoughtState state,
        ThoughtMeta metadata
) implements Serializable {
}
