package com.senars.optimizer;

import com.senars.core.*;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.senars.events.EventBus;
import com.senars.events.Events;

import java.time.Instant;
import java.util.*;

/**
 * Analyzes the performance of schemas and generates goals to optimize them.
 */
public class SchemaOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOptimizer.class);
    private static final int MIN_USES_THRESHOLD = 10;
    private static final double FAULTY_CLARITY_THRESHOLD = 0.5;
    public static final String REWRITE_SCHEMA_SYMBOLIC = "senars:rewrite_schema";
    private final EventBus eventBus;

    public SchemaOptimizer(EventBus eventBus) {
        this.eventBus = eventBus;
    }


    /**
     * A helper record to store performance metrics for a single schema.
     */
    private record SchemaPerformance(String schemaId, int uses, double totalClarity) {
        public double getAverageClarity() {
            return uses > 0 ? totalClarity / uses : 0;
        }
    }

    /**
     * Runs the schema optimization process.
     *
     * @param memory The memory to analyze.
     * @return A list of new GOAL thoughts for schemas that need optimization.
     */
    public List<Thought> run(Memory memory) {
        LOGGER.info("Running schema optimizer...");
        List<Thought> allThoughts = memory.getAllThoughts();
        Map<String, SchemaPerformance> performanceMap = analyze(allThoughts);

        List<Thought> optimizationGoals = new ArrayList<>();
        for (SchemaPerformance performance : performanceMap.values()) {
            if (performance.uses() >= MIN_USES_THRESHOLD && performance.getAverageClarity() < FAULTY_CLARITY_THRESHOLD) {
                LOGGER.warn("Schema {} is underperforming with average clarity of {} after {} uses. Generating optimization goal.",
                        performance.schemaId(), String.format("%.2f", performance.getAverageClarity()), performance.uses());

                memory.getThoughtById(performance.schemaId()).ifPresent(schemaThought -> {
                    Thought goal = createOptimizationGoal(schemaThought, performance.getAverageClarity());
                    optimizationGoals.add(goal);
                    eventBus.publish(new Events.SchemaOptimizationGoalCreatedEvent(goal));
                });
            }
        }
        return optimizationGoals;
    }

    private Map<String, SchemaPerformance> analyze(List<Thought> allThoughts) {
        Map<String, SchemaPerformance> performanceMap = new HashMap<>();
        // Initialize map with all schemas
        for (Thought thought : allThoughts) {
            if (thought.metadata().type() == ThoughtType.SCHEMA) {
                performanceMap.put(thought.id(), new SchemaPerformance(thought.id(), 0, 0.0));
            }
        }

        // Aggregate performance data from belief and report thoughts
        for (Thought thought : allThoughts) {
            if ((thought.metadata().type() == ThoughtType.BELIEF || thought.metadata().type() == ThoughtType.REPORT)
                    && thought.metadata().trace() != null) {
                for (String parentId : thought.metadata().trace()) {
                    // We are interested in the schema that was used to generate this thought.
                    // This requires a more robust way to identify the schema in the trace.
                    // For now, we assume any schema in the trace is a contributor.
                    if (performanceMap.containsKey(parentId)) {
                        SchemaPerformance current = performanceMap.get(parentId);
                        SchemaPerformance updated = new SchemaPerformance(
                                parentId,
                                current.uses() + 1,
                                current.totalClarity() + thought.state().clarity()
                        );
                        performanceMap.put(parentId, updated);
                    }
                }
            }
        }
        return performanceMap;
    }

    private Thought createOptimizationGoal(Thought faultySchema, double avgClarity) {
        String goalText = "The schema '" + faultySchema.content().text() + "' is performing poorly. " +
                "Its average output clarity is " + String.format("%.2f", avgClarity) + ". " +
                "Analyze its procedural content (prompt) and generate a new, improved version of the schema.";

        ThoughtContent content = new ThoughtContent(
                goalText,
                REWRITE_SCHEMA_SYMBOLIC, // Symbolic marker for the cognition engine
                null, // embedding
                null, // perceptual
                faultySchema.content().procedural(), // Pass the faulty procedural content for context
                null, // feedback
                null  // rules
        );
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                List.of(faultySchema.id()), // Trace back to the faulty schema
                Instant.now()
        );
        // High salience to ensure it gets processed soon
        ThoughtState state = new ThoughtState(1.0, 100.0, 1.0);

        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
