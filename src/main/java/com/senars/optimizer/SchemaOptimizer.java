package com.senars.optimizer;

import com.senars.core.*;
import com.senars.events.Events;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Analyzes the performance of schemas and generates goals to optimize them based on execution time.
 */
public class SchemaOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOptimizer.class);
    private static final int MIN_SAMPLES_THRESHOLD = 5;
    private static final long EXECUTION_TIME_THRESHOLD_MS = 2000;
    public static final String REWRITE_SCHEMA_SYMBOLIC = "senars:rewrite_schema";

    private final Memory memory;
    private final Map<String, List<Long>> schemaPerformanceData = new ConcurrentHashMap<>();

    public SchemaOptimizer(Memory memory) {
        this.memory = memory;
    }

    /**
     * Event handler for when an action has been successfully executed.
     * Collects performance data for the schema that led to the action.
     * @param event The event containing the feedback from the action.
     */
    public void onActionExecuted(Events.ActionExecutedEvent event) {
        Feedback feedback = event.feedback();
        if (feedback.status() != ActionStatus.SUCCESS) {
            return;
        }

        Thought actionPlan = feedback.actionPlan();
        if (actionPlan == null || actionPlan.metadata() == null || actionPlan.metadata().trace() == null) {
            return;
        }

        // Find the schema ID in the trace and log the performance
        actionPlan.metadata().trace().stream()
                .map(memory::getThoughtById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(thought -> thought.metadata().type() == ThoughtType.SCHEMA)
                .findFirst()
                .ifPresent(schema -> {
                    LOGGER.debug("Logging execution time for schema {}: {}ms", schema.id(), feedback.executionTimeMs());
                    schemaPerformanceData.computeIfAbsent(schema.id(), k -> new ArrayList<>())
                            .add(feedback.executionTimeMs());
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

        for (Map.Entry<String, List<Long>> entry : schemaPerformanceData.entrySet()) {
            String schemaId = entry.getKey();
            List<Long> times = entry.getValue();

            if (times.size() < MIN_SAMPLES_THRESHOLD) {
                continue;
            }

            OptionalDouble averageTimeOpt = times.stream().mapToLong(Long::longValue).average();
            if (averageTimeOpt.isEmpty()) {
                continue;
            }
            double averageTime = averageTimeOpt.getAsDouble();

            if (averageTime > EXECUTION_TIME_THRESHOLD_MS) {
                LOGGER.warn("Schema {} is underperforming with average execution time of {}ms after {} uses. Generating optimization goal.",
                        schemaId, String.format("%.2f", averageTime), times.size());

                memory.getThoughtById(schemaId).ifPresent(schemaThought -> {
                    Thought goal = createOptimizationGoal(schemaThought, averageTime);
                    optimizationGoals.add(goal);
                });
            }
        }

        if (!optimizationGoals.isEmpty()) {
            schemaPerformanceData.clear();
        }
        return optimizationGoals;
    }


    private Thought createOptimizationGoal(Thought inefficientSchema, double avgTime) {
        String goalText = String.format(
                "The schema '%s' is inefficient. Its average execution time is %.0f ms. Analyze its procedural content (prompt) and generate a new, more efficient version.",
                inefficientSchema.content().text(),
                avgTime
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
        ThoughtState state = new ThoughtState(1.0, 100.0, 1.0);

        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
