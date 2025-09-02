package com.senars.attention;

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
        var d = vecA.size();
        if (d != vecB.size() || vecA.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < d; i++) {
            var ai = vecA.get(i);
            var bi = vecB.get(i);
            dotProduct += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }

        if (normA == 0.0 || normB == 0.0)
            return 0.0;
        else
            return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Calculates the L2 norm (Euclidean magnitude) of a vector.
     *
     * @param vector The vector.
     * @return The magnitude of the vector.
     */
    public static double magnitude(List<Double> vector) {
        double norm = 0.0;
        for (Double value : vector) {
            norm += value * value;
        }
        return Math.sqrt(norm);
    }

    /**
     * Normalizes a vector to have a magnitude of 1 (a unit vector).
     *
     * @param vector The vector to normalize.
     * @return A new List containing the normalized vector. Returns the original vector if magnitude is 0.
     */
    public static List<Double> normalize(List<Double> vector) {
        double mag = magnitude(vector);
        if (mag == 0 /* Avoid division by zero */ || mag == 1)
            return vector;
        else
            return vector.stream()
                    .map(val -> val / mag)
                    .toList();
    }
}
