package com.senars.motive;

/**
 * Represents the permanent, intrinsic needs of the SeNARS system.
 * These drives provide constant, broad gradients of Salience to all related Thoughts.
 */
public enum Drive {
    /**
     * The drive to find and resolve contradictions within the Memory Nexus.
     */
    MAINTAIN_COHERENCE,

    /**
     * The drive to seek information that increases the Clarity of low-clarity Thoughts.
     */
    REDUCE_UNCERTAINTY,

    /**
     * The drive to explore novel information and synthesize new BELIEF or SCHEMA Thoughts.
     */
    ACQUIRE_KNOWLEDGE,

    /**
     * The meta-drive for self-improvement, making Thoughts about the system's own
     * performance, health, and SCHEMA efficacy inherently salient.
     */
    MAINTAIN_COGNITIVE_INTEGRITY
}
