package com.senars.salience;

import com.senars.core.*;
import com.senars.effort.EffortPredictor;
import com.senars.motive.MotiveHierarchy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SalienceCalculatorTest {

    private static final double DELTA = 1e-9;
    private SalienceCalculator calculator;
    private MotiveHierarchy motiveHierarchy;
    private EffortPredictor mockEffortPredictor;

    @BeforeEach
    void setUp() {
        mockEffortPredictor = Mockito.mock(EffortPredictor.class);
        calculator = new SalienceCalculator(mockEffortPredictor);
        motiveHierarchy = new MotiveHierarchy();
        // Default behavior for tests that don't care about effort
        when(mockEffortPredictor.predict(any(Thought.class))).thenReturn(1.0);
    }

    private Thought createTestThought(String text, List<Double> embedding, double activation, double clarity) {
        return new Thought(
            UUID.randomUUID().toString(),
            new ThoughtContent(text, null, embedding, null, null, null),
            new ThoughtState(clarity, 0, activation), // Initial salience is 0, it's what we calculate
            new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    private Thought createTestGoal(String text, List<Double> embedding) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(text, null, embedding, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMetadata(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now())
        );
    }

    @Test
    void testCalculate_noEmbedding_noGoals() {
        Thought thought = createTestThought("test", null, 0.5, 0.8);
        double salience = calculator.calculate(thought, motiveHierarchy);
        // Expected: (activation + motiveBonus) * clarity / effort = (0.5 + 0) * 0.8 / 1.0 = 0.4
        assertEquals(0.4, salience, DELTA);
    }

    @Test
    void testCalculate_withAmbitionMatch() {
        List<Double> embedding = List.of(1.0, 0.0);
        Thought ambition = createTestGoal("match", embedding);
        motiveHierarchy.addAmbition(ambition);

        Thought thoughtToScore = createTestThought("test", embedding, 0.5, 0.8);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);
        // Motive bonus should be 1.0 (perfect match)
        // Expected: (0.5 + 1.0) * 0.8 / 1.0 = 1.5 * 0.8 = 1.2
        assertEquals(1.2, salience, DELTA);
    }

    @Test
    void testCalculate_withIntentionMatch() {
        List<Double> embedding = List.of(1.0, 0.0);
        Thought intention = createTestGoal("match", embedding);
        motiveHierarchy.setIntention(intention);

        Thought thoughtToScore = createTestThought("test", embedding, 0.2, 0.5);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);
        // Motive bonus should be 1.0
        // Expected: (0.2 + 1.0) * 0.5 / 1.0 = 1.2 * 0.5 = 0.6
        assertEquals(0.6, salience, DELTA);
    }

    @Test
    void testCalculate_noMatch() {
        List<Double> embedding1 = List.of(1.0, 0.0);
        List<Double> embedding2 = List.of(0.0, 1.0); // Orthogonal
        Thought ambition = createTestGoal("goal", embedding1);
        motiveHierarchy.addAmbition(ambition);

        Thought thoughtToScore = createTestThought("test", embedding2, 0.5, 0.8);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);
        // Motive bonus should be 0.0
        // Expected: (0.5 + 0.0) * 0.8 / 1.0 = 0.4
        assertEquals(0.4, salience, DELTA);
    }

    @Test
    void testCalculate_choosesMaxMotiveBonus() {
        List<Double> embedding1 = List.of(1.0, 0.0, 0.0); // Perfect match with thought
        List<Double> embedding2 = List.of(0.0, 1.0, 0.0); // Orthogonal
        List<Double> embedding3 = List.of(0.8, 0.6, 0.0); // Partial match

        Thought ambition1 = createTestGoal("ambition1", embedding2); // a miss
        Thought ambition2 = createTestGoal("ambition2", embedding3); // a partial hit
        motiveHierarchy.addAmbition(ambition1);
        motiveHierarchy.addAmbition(ambition2);

        Thought intention = createTestGoal("intention", embedding1); // a perfect hit
        motiveHierarchy.setIntention(intention);

        Thought thoughtToScore = createTestThought("test", embedding1, 0.1, 1.0);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);
        // Motive bonus should be 1.0 (from the intention, which is the max)
        // Expected: (0.1 + 1.0) * 1.0 / 1.0 = 1.1
        assertEquals(1.1, salience, DELTA);
    }

    @Test
    void testCalculate_withVariableEffort() {
        Thought thought = createTestThought("test", null, 0.5, 0.8);
        when(mockEffortPredictor.predict(thought)).thenReturn(2.0);

        double salience = calculator.calculate(thought, motiveHierarchy);
        // Expected: (activation + motiveBonus) * clarity / effort = (0.5 + 0) * 0.8 / 2.0 = 0.2
        assertEquals(0.2, salience, DELTA);
    }
}
