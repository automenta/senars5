package com.senars.optimizer;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.effort.EffortRecord;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Analyzes the performance of the effort prediction model and triggers
 * self-optimization when the model's predictions are inaccurate.
 */
public class EffortModelOptimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EffortModelOptimizer.class);
    private static final int MIN_RECORDS_FOR_ANALYSIS = 20;
    private static final double ERROR_THRESHOLD = 0.5; // Trigger if average error is > 50%
    public static final String REWRITE_EFFORT_MODEL_SYMBOLIC = "senars:rewrite_effort_model_schema";

    private final Memory memory;
    private final EventBus eventBus;

    public EffortModelOptimizer(Memory memory, EventBus eventBus) {
        this.memory = memory;
        this.eventBus = eventBus;
    }

    /**
     * Runs the optimization analysis.
     *
     * @param effortTracker The tracker containing the effort data.
     * @return A list of new goals created, if any.
     */
    public List<Thought> run(EffortTracker effortTracker) {
        List<EffortRecord> records = effortTracker.drainRecords();
        if (records.size() < MIN_RECORDS_FOR_ANALYSIS) {
            return Collections.emptyList(); // Not enough data to make a decision
        }

        double totalAbsolutePercentageError = 0.0;
        for (EffortRecord record : records) {
            if (record.actualEffort() > 0) {
                double error = Math.abs(record.predictedEffort() - record.actualEffort());
                totalAbsolutePercentageError += error / record.actualEffort();
            }
        }
        double meanAbsolutePercentageError = totalAbsolutePercentageError / records.size();

        LOGGER.info("Analyzed {} effort records. Mean Absolute Percentage Error (MAPE): {}%",
                records.size(), String.format("%.2f", meanAbsolutePercentageError * 100));

        if (meanAbsolutePercentageError > ERROR_THRESHOLD) {
            LOGGER.warn("Effort prediction MAPE ({}) exceeds threshold ({}). Creating goal to optimize model.",
                    String.format("%.2f", meanAbsolutePercentageError), String.format("%.2f", ERROR_THRESHOLD));
            Thought optimizationGoal = createOptimizationGoal(records, meanAbsolutePercentageError);
            if (optimizationGoal != null) {
                return List.of(optimizationGoal);
            }
        }

        return Collections.emptyList();
    }

    private Thought createOptimizationGoal(List<EffortRecord> records, double error) {
        // 1. Find the current effort model schema
        Optional<Thought> effortModelOpt = memory.findSchemaBySymbolicName(com.senars.effort.EffortPredictor.EFFORT_MODEL_SCHEMA_NAME);
        if (effortModelOpt.isEmpty()) {
            LOGGER.error("Could not find the effort model schema '{}' to create an optimization goal.", com.senars.effort.EffortPredictor.EFFORT_MODEL_SCHEMA_NAME);
            // In a real system, we might create a goal to *create* the schema, but for now we'll just log.
            return null;
        }
        Thought effortModelSchema = effortModelOpt.get();

        // 2. Create the goal text
        String goalText = String.format(
                "The effort prediction model '%s' is performing poorly with an average error of %.2f%%. " +
                "Analyze its procedural content and the %d recent performance records to generate a new, more accurate model.",
                effortModelSchema.content().text(),
                error * 100,
                records.size()
        );

        // 3. A future implementation could serialize the records to JSON and include them in the procedural content.
        // For now, we pass the old model's procedural content.
        ThoughtContent content = new ThoughtContent(
                goalText,
                REWRITE_EFFORT_MODEL_SYMBOLIC,
                null, null,
                effortModelSchema.content().procedural(), // Pass the old model's content
                null, null
        );

        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                List.of(effortModelSchema.id()), // Trace back to the old model
                Instant.now()
        );
        ThoughtState state = new ThoughtState(1.0, 120.0, 1.0); // High salience

        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
