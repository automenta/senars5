package com.senars.logic.mdr;

import com.senars.core.Feedback;
import com.senars.systems.Memory;

/**
 * A monitor configuration for memory curation.
 * This monitor listens for "poor context retrieval" errors from the UCR's backward pass
 * and triggers memory curation processes.
 */
public class MemoryCurationMonitorConfig extends MonitorConfig {

    private final Memory memory;

    public MemoryCurationMonitorConfig(Memory memory) {
        super(
                "MemoryCurationMonitor",
                feedback -> feedback.status() == com.senars.core.ActionStatus.FAILURE
                        && feedback.output() != null
                        && feedback.output().toLowerCase().contains("context retrieval"),
                "Monitors for poor context retrieval errors and triggers memory curation"
        );
        this.memory = memory;
    }

    /**
     * Checks if a failed action is related to poor context retrieval.
     *
     * @param feedback The feedback from a failed action
     * @return true if the failure is related to context retrieval, false otherwise
     */
    public boolean isContextRetrievalFailure(Feedback feedback) {
        // Check if the failure message indicates a context retrieval issue
        return feedback.output() != null &&
                feedback.output().toLowerCase().contains("context retrieval");
    }
}