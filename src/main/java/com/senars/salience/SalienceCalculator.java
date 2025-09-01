package com.senars.salience;

import com.senars.core.Thought;
import com.senars.effort.EffortPredictor;
import com.senars.motive.MotiveHierarchy;
import java.util.List;
import java.util.Optional;

/**
 * Calculates the Salience score for a given Thought.
 */
public class SalienceCalculator {

    private final EffortPredictor effortPredictor;

    public SalienceCalculator(EffortPredictor effortPredictor) {
        this.effortPredictor = effortPredictor;
    }

    /**
     * Calculates the salience of a Thought based on the formula:
     * Salience = (Activation + MotiveBonus) * Clarity / PredictedEffort
     *
     * @param thought The Thought to calculate salience for.
     * @param motiveHierarchy The system's current motive hierarchy.
     * @return The calculated salience score.
     */
    public double calculate(Thought thought, MotiveHierarchy motiveHierarchy) {
        double activation = thought.state().activation();
        double clarity = thought.state().clarity();

        double motiveBonus = calculateMotiveBonus(thought, motiveHierarchy);
        double predictedEffort = effortPredictor.predict(thought);

        return (activation + motiveBonus) * clarity / predictedEffort;
    }

    private double calculateMotiveBonus(Thought thought, MotiveHierarchy motiveHierarchy) {
        List<Double> thoughtEmbedding = thought.content().embedding();
        if (thoughtEmbedding == null || thoughtEmbedding.isEmpty()) {
            return 0.0;
        }

        double maxSimilarity = 0.0;

        // Compare with intention
        Optional<Thought> intentionOpt = motiveHierarchy.getIntention();
        if (intentionOpt.isPresent()) {
            List<Double> intentionEmbedding = intentionOpt.get().content().embedding();
            if (intentionEmbedding != null && !intentionEmbedding.isEmpty()) {
                maxSimilarity = Math.max(maxSimilarity, VectorMath.cosineSimilarity(thoughtEmbedding, intentionEmbedding));
            }
        }

        // Compare with ambitions
        for (Thought ambition : motiveHierarchy.getAmbitions()) {
            List<Double> ambitionEmbedding = ambition.content().embedding();
            if (ambitionEmbedding != null && !ambitionEmbedding.isEmpty()) {
                maxSimilarity = Math.max(maxSimilarity, VectorMath.cosineSimilarity(thoughtEmbedding, ambitionEmbedding));
            }
        }

        // Compare with drives
        for (Thought drive : motiveHierarchy.getDrives()) {
            List<Double> driveEmbedding = drive.content().embedding();
            if (driveEmbedding != null && !driveEmbedding.isEmpty()) {
                maxSimilarity = Math.max(maxSimilarity, VectorMath.cosineSimilarity(thoughtEmbedding, driveEmbedding));
            }
        }

        return maxSimilarity;
    }
}
