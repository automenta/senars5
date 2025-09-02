package com.senars.salience;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtType;
import com.senars.effort.EffortPredictor;
import com.senars.motive.MotiveHierarchy;

import java.util.List;
import java.util.Optional;

/**
 * Calculates the Salience score for a given Thought.
 */
public class SalienceCalculator {

    private static final String REDUCE_UNCERTAINTY_DRIVE_ID = "drive-reduceuncertainty";
    private static final String ENRICH_KNOWLEDGE_DRIVE_ID = "drive-enrichknowledge";
    private static final double UNCERTAINTY_BONUS_MULTIPLIER = 50.0;
    private static final double ENRICHMENT_BONUS = 25.0; // A fixed bonus for thoughts that need enrichment

    private final EffortPredictor effortPredictor;

    public SalienceCalculator(EffortPredictor effortPredictor) {
        this.effortPredictor = effortPredictor;
    }

    /**
     * Calculates the salience of a Thought based on the formula:
     * Salience = (Activation + MotiveBonus) * Clarity / PredictedEffort
     *
     * @param thought         The Thought to calculate salience for.
     * @param motiveHierarchy The system's current motive hierarchy.
     * @return The calculated salience score.
     */
    public double calculate(Thought thought, MotiveHierarchy motiveHierarchy) {
        double activation = thought.state().activation();
        double clarity = thought.state().clarity();

        // The motive bonus is now a combination of goal-oriented bonus and drive-based bonuses
        double motiveBonus = calculateMotiveBonus(thought, motiveHierarchy);
        double predictedEffort = effortPredictor.predict(thought);

        // Avoid division by zero or negative effort, which would invalidate salience
        if (predictedEffort <= 0) {
            predictedEffort = 1.0;
        }

        return (activation + motiveBonus) * clarity / predictedEffort;
    }

    private double calculateMotiveBonus(Thought thought, MotiveHierarchy motiveHierarchy) {
        double goalBonus = calculateGoalBonus(thought, motiveHierarchy);
        double driveBonus = calculateDriveBonus(thought, motiveHierarchy);

        return goalBonus + driveBonus;
    }

    private double calculateGoalBonus(Thought thought, MotiveHierarchy motiveHierarchy) {
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
        return maxSimilarity;
    }

    private double calculateDriveBonus(Thought thought, MotiveHierarchy motiveHierarchy) {
        double totalDriveBonus = 0.0;

        for (Thought drive : motiveHierarchy.getDrives()) {
            // Ensure we are only processing actual DRIVE thoughts
            if (drive.metadata().type() != ThoughtType.DRIVE) {
                continue;
            }

            // Special logic for the Reduce Uncertainty drive
            if (REDUCE_UNCERTAINTY_DRIVE_ID.equals(drive.id())) {
                // This drive adds a bonus to thoughts with low clarity.
                // The bonus is inversely proportional to clarity.
                double clarity = thought.state().clarity();
                if (clarity < 1.0) {
                    totalDriveBonus += UNCERTAINTY_BONUS_MULTIPLIER * (1.0 - clarity);
                }
            } else {
                var thoughtContent = thought.content();
                if (ENRICH_KNOWLEDGE_DRIVE_ID.equals(drive.id())) {
                    // This drive adds a bonus to thoughts that have text but are missing an embedding.
                    boolean needsEmbedding = thoughtContent.text() != null &&
                            !thoughtContent.text().isEmpty() &&
                            (thoughtContent.embedding() == null || thoughtContent.embedding().isEmpty());
                    if (needsEmbedding) {
                        totalDriveBonus += ENRICHMENT_BONUS;
                    }
                } else {
                    // For all other drives, the bonus is based on semantic similarity.
                    List<Double> thoughtEmbedding = thoughtContent.embedding();
                    List<Double> driveEmbedding = drive.content().embedding();

                    if (thoughtEmbedding != null && !thoughtEmbedding.isEmpty() && driveEmbedding != null && !driveEmbedding.isEmpty()) {
                        totalDriveBonus += VectorMath.cosineSimilarity(thoughtEmbedding, driveEmbedding);
                    }
                }
            }
        }

        return totalDriveBonus;
    }
}
