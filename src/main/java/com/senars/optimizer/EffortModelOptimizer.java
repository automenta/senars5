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
            return List.of(createOptimizationGoal(records, meanAbsolutePercentageError));
        }

        return Collections.emptyList();
    }

    private Thought createOptimizationGoal(List<EffortRecord> records, double error) {
        // For now, we'll just put a summary in the goal text.
        // A more advanced implementation could store the full record data in a structured format.
        String goalText = String.format(
                "The effort prediction model is performing poorly with an average error of %.2f%%. " +
                "Analyze the recent performance data and generate a new, more accurate effort prediction model schema. " +
                "There are %d records to analyze.",
                error * 100,
                records.size()
        );

        // A future implementation could serialize the records to JSON and include them.
        ThoughtContent content = new ThoughtContent(goalText, REWRITE_EFFORT_MODEL_SYMBOLIC, null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                Collections.emptyList(),
                Instant.now()
        );
        // High salience to prioritize self-improvement
        ThoughtState state = new ThoughtState(1.0, 120.0, 1.0);

        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
