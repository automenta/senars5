package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.systems.IGroundingSystem;

/**
 * An in-memory implementation of the IGroundingSystem that simply logs
 * the feedback it receives to standard output.
 * Useful for verifying that the feedback loop is being triggered.
 */
public class LoggingGroundingSystem implements IGroundingSystem {

    @Override
    public void processFeedback(Thought feedbackReport) {
        System.out.println("[LoggingGroundingSystem] Received feedback report: " + feedbackReport.id());
        // In a real implementation, this method would parse the report
        // and adjust the clarity of thoughts in the feedbackReport's provenance trace.
    }
}
