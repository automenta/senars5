package com.senars.core;

import java.io.Serializable;

/**
 * Types of causal relations between thoughts in the cognitive graph.
 */
public enum CausalRelationType implements Serializable {
    /**
     * Direct causation: A directly caused B
     */
    DIRECT_CAUSATION,

    /**
     * Indirect causation: A indirectly caused B through intermediate steps
     */
    INDIRECT_CAUSATION,

    /**
     * Logical inference: B was logically derived from A
     */
    LOGICAL_INFERENCE,

    /**
     * Associative link: A and B are associated in memory but not causally linked
     */
    ASSOCIATIVE,

    /**
     * Counterfactual: In a simulation, if A had happened, B would have been the result
     */
    COUNTERFACTUAL,

    /**
     * Temporal sequence: A happened before B, but causality is not established
     */
    TEMPORAL_SEQUENCE
}