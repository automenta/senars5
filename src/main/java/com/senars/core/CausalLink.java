package com.senars.core;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a causal link between two thoughts in the cognitive graph.
 * This replaces the linear trace array with a more expressive graph structure.
 */
public record CausalLink(
        String sourceThoughtId,
        String targetThoughtId,
        CausalRelationType relationType,
        double strength
) implements Serializable {
    
    public CausalLink {
        Objects.requireNonNull(sourceThoughtId, "sourceThoughtId cannot be null");
        Objects.requireNonNull(targetThoughtId, "targetThoughtId cannot be null");
        Objects.requireNonNull(relationType, "relationType cannot be null");
        if (strength < 0.0 || strength > 1.0) {
            throw new IllegalArgumentException("strength must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Creates a causal link with default strength of 1.0
     */
    public CausalLink(String sourceThoughtId, String targetThoughtId, CausalRelationType relationType) {
        this(sourceThoughtId, targetThoughtId, relationType, 1.0);
    }
}