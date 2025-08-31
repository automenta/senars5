package com.senars.salience;

import java.util.List;

/**
 * A utility class for performing mathematical operations on vectors.
 */
public final class VectorMath {

    private VectorMath() {
        // Private constructor to prevent instantiation of utility class.
    }

    /**
     * Calculates the cosine similarity between two vectors.
     *
     * @param vecA The first vector.
     * @param vecB The second vector.
     * @return The cosine similarity, a value between -1.0 and 1.0. Returns 0.0 if
     *         vectors are of different sizes or if either vector has a magnitude of 0.
     * @throws NullPointerException if either vector is null.
     */
    public static double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA.size() != vecB.size() || vecA.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            dotProduct += vecA.get(i) * vecB.get(i);
            normA += vecA.get(i) * vecA.get(i);
            normB += vecB.get(i) * vecB.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
