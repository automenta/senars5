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
    private static final String REDUCE_UNCERTAINTY_DRIVE_ID = "drive-reduceuncertainty";


    @BeforeEach
    void setUp() {
        mockEffortPredictor = Mockito.mock(EffortPredictor.class);
        // Default behavior for tests that don't care about effort
        when(mockEffortPredictor.predict(any(Thought.class))).thenReturn(1.0);
    }

    private Thought createTestThought(String text, List<Double> embedding, double activation, double clarity) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(text, null, embedding, null, null, null),
                new ThoughtState(clarity, 0, activation), // Initial salience is 0, it's what we calculate
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    private Thought createTestGoal(String id, List<Double> embedding) {
        return new Thought(
                id,
                new ThoughtContent("goal", null, embedding, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now())
        );
    }

    private Thought createTestDrive(String id, String text, List<Double> embedding) {
        return new Thought(
                id,
                new ThoughtContent(text, null, embedding, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.DRIVE, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }

    @Test
    void testCalculate_onlySemanticDriveBonus() {
        List<Double> embedding = List.of(1.0, 0.0);
        Thought drive = createTestDrive("drive-acquire-knowledge", "knowledge", embedding);
        motiveHierarchy = new MotiveHierarchy(List.of(drive));
        calculator = new SalienceCalculator(mockEffortPredictor);

        Thought thoughtToScore = createTestThought("test", embedding, 0.5, 0.8);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);

        // GoalBonus = 0. DriveBonus = 1.0 (perfect match). Total MotiveBonus = 1.0
        // Expected: (activation + motiveBonus) * clarity / effort = (0.5 + 1.0) * 0.8 / 1.0 = 1.2
        assertEquals(1.2, salience, DELTA);
    }

    @Test
    void testCalculate_onlyReduceUncertaintyDriveBonus() {
        Thought drive = createTestDrive(REDUCE_UNCERTAINTY_DRIVE_ID, "uncertainty", null); // No embedding needed
        motiveHierarchy = new MotiveHierarchy(List.of(drive));
        calculator = new SalienceCalculator(mockEffortPredictor);

        // Thought with low clarity
        Thought thoughtToScore = createTestThought("test", List.of(1.0, 0.0), 0.5, 0.6);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);

        // GoalBonus = 0.
        // DriveBonus = 50.0 * (1.0 - 0.6) = 50.0 * 0.4 = 20.0
        // Total MotiveBonus = 20.0
        // Expected: (activation + motiveBonus) * clarity / effort = (0.5 + 20.0) * 0.6 / 1.0 = 20.5 * 0.6 = 12.3
        assertEquals(12.3, salience, DELTA);
    }

    @Test
    void testCalculate_reduceUncertaintyDoesNotApplyForHighClarity() {
        Thought drive = createTestDrive(REDUCE_UNCERTAINTY_DRIVE_ID, "uncertainty", null);
        motiveHierarchy = new MotiveHierarchy(List.of(drive));
        calculator = new SalienceCalculator(mockEffortPredictor);

        // Thought with perfect clarity
        Thought thoughtToScore = createTestThought("test", List.of(1.0, 0.0), 0.5, 1.0);
        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);

        // GoalBonus = 0. DriveBonus = 0 (clarity is 1.0). Total MotiveBonus = 0.
        // Expected: (activation + motiveBonus) * clarity / effort = (0.5 + 0.0) * 1.0 / 1.0 = 0.5
        assertEquals(0.5, salience, DELTA);
    }


    @Test
    void testCalculate_goalBonusAndDriveBonusAreAdded() {
        List<Double> embedding1 = List.of(1.0, 0.0, 0.0);
        List<Double> embedding2 = List.of(0.0, 1.0, 0.0);

        // Setup Goals
        Thought ambition = createTestGoal("ambition1", embedding1); // Perfect match with thought

        // Setup Drives
        Thought drive = createTestDrive("drive-acquire-knowledge", "knowledge", embedding2); // Perfect match with thought's other dimension

        motiveHierarchy = new MotiveHierarchy(List.of(drive));
        motiveHierarchy.addAmbition(ambition);
        calculator = new SalienceCalculator(mockEffortPredictor);

        // A thought that matches both the ambition and the drive, but on different dimensions
        List<Double> thoughtEmbedding = VectorMath.normalize(List.of(1.0, 1.0, 0.0));
        Thought thoughtToScore = createTestThought("test", thoughtEmbedding, 0.2, 0.9);

        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);

        // GoalBonus = cos(thought, ambition) = 1/sqrt(2) ~ 0.7071
        // DriveBonus = cos(thought, drive) = 1/sqrt(2) ~ 0.7071
        // Total MotiveBonus = ~1.4142
        double expectedMotiveBonus = 1.0 / Math.sqrt(2) + 1.0 / Math.sqrt(2);
        // Expected: (activation + motiveBonus) * clarity / effort
        // (0.2 + 1.41421356) * 0.9 / 1.0 = 1.61421356 * 0.9 = 1.4527922
        double expectedSalience = (0.2 + expectedMotiveBonus) * 0.9;

        assertEquals(expectedSalience, salience, DELTA);
    }

    @Test
    void testCalculate_fullHouse_goalAndMultipleDrives() {
        List<Double> embeddingGoal = List.of(1.0, 0.0, 0.0);
        List<Double> embeddingDrive1 = List.of(0.0, 1.0, 0.0);
        List<Double> embeddingThought = VectorMath.normalize(List.of(1.0, 1.0, 0.0)); // Matches goal and drive1

        // Setup Goals
        Thought ambition = createTestGoal("ambition1", embeddingGoal);

        // Setup Drives
        Thought drive1 = createTestDrive("drive-acquire-knowledge", "knowledge", embeddingDrive1);
        Thought drive2 = createTestDrive(REDUCE_UNCERTAINTY_DRIVE_ID, "uncertainty", null);

        motiveHierarchy = new MotiveHierarchy(List.of(drive1, drive2));
        motiveHierarchy.addAmbition(ambition);
        calculator = new SalienceCalculator(mockEffortPredictor);

        // A thought with low clarity that matches a goal and a drive
        Thought thoughtToScore = createTestThought("test", embeddingThought, 0.1, 0.5);

        double salience = calculator.calculate(thoughtToScore, motiveHierarchy);

        // GoalBonus = cos(thought, ambition) = 1/sqrt(2) ~ 0.7071
        double goalBonus = 1.0 / Math.sqrt(2);
        // DriveBonus (semantic) = cos(thought, drive1) = 1/sqrt(2) ~ 0.7071
        double driveBonusSemantic = 1.0 / Math.sqrt(2);
        // DriveBonus (uncertainty) = 50.0 * (1.0 - 0.5) = 25.0
        double driveBonusUncertainty = 50.0 * 0.5;
        // Total MotiveBonus = ~0.7071 + ~0.7071 + 25.0 = ~26.4142
        double totalMotiveBonus = goalBonus + driveBonusSemantic + driveBonusUncertainty;

        // Expected: (activation + motiveBonus) * clarity / effort
        // (0.1 + 26.4142) * 0.5 / 1.0 = 26.5142 * 0.5 = 13.2571
        double expectedSalience = (0.1 + totalMotiveBonus) * 0.5;
        assertEquals(expectedSalience, salience, DELTA);
    }
}
