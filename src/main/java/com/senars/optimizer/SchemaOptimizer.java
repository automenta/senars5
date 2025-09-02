package com.senars.optimizer;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analyzes the performance of schemas and generates goals to optimize them based on execution time.
 */
public class SchemaOptimizer {

    public static final String REWRITE_SCHEMA_SYMBOLIC = "senars:rewrite_schema";
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOptimizer.class);
    private static final int MIN_SAMPLES_THRESHOLD = 10;
    private static final long EXECUTION_TIME_THRESHOLD_MS = 1500;
    private final Memory memory;
    private final EventBus eventBus;
    private final Map<String, SchemaPerformanceTracker> schemaPerformanceData = new ConcurrentHashMap<>();

    public SchemaOptimizer(Memory memory, EventBus eventBus) {
        this.memory = memory;
        this.eventBus = eventBus;
    }

    /**
     * Event handler for when an action has been successfully executed.
     * Collects performance data for the schema that led to the action.
     * @param event The event containing the feedback from the action.
     */
    public void onActionExecuted(Events.ActionExecutedEvent event) {
        Feedback feedback = event.feedback();
        Thought actionPlan = feedback.actionPlan();

        if (actionPlan == null || actionPlan.metadata() == null || actionPlan.metadata().trace() == null) {
            return;
        }

        // Find the schema ID in the trace and log the performance
        actionPlan.metadata().trace().stream()
                .findFirst() // The immediate parent is the most relevant schema
                .flatMap(memory::getThoughtById)
                .filter(thought -> thought.metadata().type() == ThoughtType.SCHEMA)
                .ifPresent(schema -> {
                    LOGGER.debug("Logging execution time for schema {}: {}ms, Status: {}", schema.id(), feedback.executionTimeMs(), feedback.status());
                    schemaPerformanceData.computeIfAbsent(schema.id(), k -> new SchemaPerformanceTracker())
                            .addExecution(feedback.executionTimeMs(), feedback.status() == ActionStatus.SUCCESS);
                });
    }

    /**
     * Runs the schema optimization process based on collected performance data.
     * This method is intended to be called periodically by the CognitiveCycle.
     * @return A list of new GOAL thoughts for schemas that need optimization.
     */
    public List<Thought> run() {
        LOGGER.debug("Running schema optimizer on collected performance data...");
        List<Thought> optimizationGoals = new ArrayList<>();

        for (Map.Entry<String, SchemaPerformanceTracker> entry : schemaPerformanceData.entrySet()) {
            String schemaId = entry.getKey();
            SchemaPerformanceTracker tracker = entry.getValue();

            if (tracker.getTotalExecutions() < MIN_SAMPLES_THRESHOLD) {
                continue;
            }

            double averageTime = tracker.getAverageExecutionTime();
            double successRate = tracker.getSuccessRate();

            // Check for high execution time or low success rate
            if (averageTime > EXECUTION_TIME_THRESHOLD_MS || successRate < 0.5) {
                LOGGER.warn("Schema {} is underperforming (AvgTime: {}ms, SuccessRate: {}%, Executions: {}). Generating optimization goal.",
                        schemaId, String.format("%.2f", averageTime), String.format("%.2f", successRate * 100), tracker.getTotalExecutions());

                memory.getThoughtById(schemaId).ifPresent(schemaThought -> {
                    Thought goal = createOptimizationGoal(schemaThought, averageTime, successRate);
                    optimizationGoals.add(goal);
                    eventBus.publish(new Events.SchemaOptimizationGoalCreatedEvent(goal));
                });

                // Reset the tracker for this schema after generating a goal
                tracker.reset();
            }
        }

        return optimizationGoals;
    }


    private Thought createOptimizationGoal(Thought inefficientSchema, double avgTime, double successRate) {
        String reason = avgTime > EXECUTION_TIME_THRESHOLD_MS ?
                String.format("its average execution time is %.0f ms", avgTime) :
                String.format("its success rate is only %.0f%%", successRate * 100);

        String goalText = String.format(
                "The schema '%s' is inefficient because %s. Analyze its procedural content and generate a new, more efficient version.",
                inefficientSchema.content().text(),
                reason
        );

        ThoughtContent content = new ThoughtContent(
                goalText,
                REWRITE_SCHEMA_SYMBOLIC,
                null, null,
                inefficientSchema.content().procedural(),
                null, null
        );
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                List.of(inefficientSchema.id()),
                Instant.now()
        );
        ThoughtState state = new ThoughtState(1.0, 100.0, 1.0); // High salience

        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
