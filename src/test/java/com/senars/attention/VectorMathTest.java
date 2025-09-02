package com.senars.attention;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorMathTest {

    private static final double DELTA = 1e-9;

    @Test
    void testCosineSimilarity_identicalVectors() {
        List<Double> v1 = List.of(5.0, 2.0, 8.0);
        assertEquals(1.0, VectorMath.cosineSimilarity(v1, v1), DELTA);
    }

    @Test
    void testCosineSimilarity_orthogonalVectors() {
        List<Double> v1 = List.of(1.0, 0.0);
        List<Double> v2 = List.of(0.0, 1.0);
        assertEquals(0.0, VectorMath.cosineSimilarity(v1, v2), DELTA);
    }

    @Test
    void testCosineSimilarity_oppositeVectors() {
        List<Double> v1 = List.of(1.0, 2.0, 3.0);
        List<Double> v2 = List.of(-1.0, -2.0, -3.0);
        assertEquals(-1.0, VectorMath.cosineSimilarity(v1, v2), DELTA);
    }

    @Test
    void testCosineSimilarity_generalCase() {
        List<Double> v1 = List.of(3.0, 2.0, 0.0, 5.0);
        List<Double> v2 = List.of(1.0, 0.0, 0.0, 0.0);
        // cos(theta) = (3*1) / (sqrt(9+4+25) * sqrt(1)) = 3 / sqrt(38)
        double expected = 3.0 / Math.sqrt(38.0);
        assertEquals(expected, VectorMath.cosineSimilarity(v1, v2), DELTA);
    }

    @Test
    void testCosineSimilarity_differentLengthVectors() {
        List<Double> v1 = List.of(1.0, 0.0);
        List<Double> v2 = List.of(1.0, 0.0, 0.0);
        assertEquals(0.0, VectorMath.cosineSimilarity(v1, v2), DELTA);
    }

    @Test
    void testCosineSimilarity_zeroVector() {
        List<Double> v1 = List.of(1.0, 2.0);
        List<Double> v2 = List.of(0.0, 0.0);
        assertEquals(0.0, VectorMath.cosineSimilarity(v1, v2), DELTA);
    }

    @Test
    void testCosineSimilarity_emptyVectors() {
        List<Double> v1 = Collections.emptyList();
        List<Double> v2 = Collections.emptyList();
        assertEquals(0.0, VectorMath.cosineSimilarity(v1, v2), DELTA);
    }
}
