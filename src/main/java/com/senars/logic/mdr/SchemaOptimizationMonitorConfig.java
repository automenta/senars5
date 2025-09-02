package com.senars.logic.mdr;

import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.systems.Memory;

/**
 * A specialized monitor configuration for schema optimization.
 * This monitor watches for failed actions and traces them back to schemas
 * to identify schema-related issues that need optimization.
 */
public class SchemaOptimizationMonitorConfig extends MonitorConfig {

    private final Memory memory;

    public SchemaOptimizationMonitorConfig(Memory memory) {
        super(
                "SchemaOptimizationMonitor",
                feedback -> feedback.status() == com.senars.core.ActionStatus.FAILURE,
                "Monitors for failed action executions and traces them back to schemas for optimization"
        );
        this.memory = memory;
    }

    /**
     * Checks if a failed action is related to a schema by tracing back through the causal chain.
     *
     * @param feedback The feedback from a failed action
     * @return true if the failure is related to a schema, false otherwise
     */
    public boolean isSchemaRelatedFailure(Feedback feedback) {
        // Get the action plan
        Thought actionPlan = feedback.actionPlan();
        if (actionPlan == null || actionPlan.metadata() == null || actionPlan.metadata().trace() == null) {
            return false;
        }

        // Look for a schema in the trace
        return actionPlan.metadata().trace().stream()
                .findFirst()
                .flatMap(memory::getThoughtById)
                .filter(thought -> thought.metadata().type() == ThoughtType.SCHEMA)
                .isPresent();
    }
}