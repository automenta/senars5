package com.senars.optimizer;

/**
 * Tracks performance metrics for a schema.
 * This class is not thread-safe and should be synchronized externally if needed.
 */
class SchemaPerformanceTracker {
    private long totalExecutionTime = 0;
    private int successCount = 0;
    private int failureCount = 0;

    /**
     * Adds a new execution record.
     * @param executionTimeMs The execution time in milliseconds.
     * @param wasSuccessful True if the execution was successful, false otherwise.
     */
    public void addExecution(long executionTimeMs, boolean wasSuccessful) {
        this.totalExecutionTime += executionTimeMs;
        if (wasSuccessful) {
            successCount++;
        } else {
            failureCount++;
        }
    }

    /**
     * @return The total number of times this schema has been used.
     */
    public int getTotalExecutions() {
        return successCount + failureCount;
    }

    /**
     * @return The success rate of this schema as a value between 0.0 and 1.0.
     */
    public double getSuccessRate() {
        int total = getTotalExecutions();
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount / total;
    }

    /**
     * @return The average execution time in milliseconds.
     */
    public double getAverageExecutionTime() {
        int total = getTotalExecutions();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalExecutionTime / total;
    }

    /**
     * Resets all performance metrics to their initial state.
     */
    public void reset() {
        totalExecutionTime = 0;
        successCount = 0;
        failureCount = 0;
    }
}
