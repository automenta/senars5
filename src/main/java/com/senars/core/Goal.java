package com.senars.core;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a high-level goal in the system's motive hierarchy.
 * Goals are distinct from Thoughts; they are long-term objectives that drive the generation of tasks.
 *
 * @param id          A unique identifier for the Goal.
 * @param description A natural language description of what the goal is.
 * @param status      The current status of the goal (e.g., ACTIVE, COMPLETED).
 * @param priority    A value indicating the importance of the goal, used for planning.
 */
public record Goal(
        String id,
        String description,
        Status status,
        double priority
) implements Serializable {

    public Goal {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        if (priority < 0.0) {
            throw new IllegalArgumentException("priority cannot be negative");
        }
    }

    /**
     * The lifecycle status of a Goal.
     */
    public enum Status {
        /**
         * The goal is actively being pursued.
         */
        ACTIVE,
        /**
         * The goal has been successfully achieved.
         */
        COMPLETED,
        /**
         * The goal has been abandoned and is no longer being pursued.
         */
        ABANDONED,
        /**
         * The goal is waiting for a dependency to be met before it can become active.
         */
        BLOCKED
    }
}
