package com.senars.effort;

import com.senars.core.*;
import com.senars.systems.IMemoryNexus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class EffortPredictorTest {

    private static final double DELTA = 1e-9;
    private IMemoryNexus mockMemoryNexus;
    private EffortPredictor predictor;

    @BeforeEach
    void setUp() {
        mockMemoryNexus = Mockito.mock(IMemoryNexus.class);
        predictor = new EffortPredictor(mockMemoryNexus);
    }

    private Thought createTestThought(String text) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(text, null, null, null, null, null),
                new ThoughtState(1.0, 0, 1.0),
                new ThoughtMetadata(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );
    }

    @Test
    void predict_whenSchemaFound_usesModelFromSchema() {
        // Arrange
        IEffortPredictionModel customModel = new LinearTextEffortModel(0.5, 5.0);
        Thought schemaThought = createSchemaThought(customModel);
        when(mockMemoryNexus.findSchemaBySymbolicName(EffortPredictor.EFFORT_MODEL_SCHEMA_NAME))
                .thenReturn(Optional.of(schemaThought));

        Thought thoughtToPredict = createTestThought("hello world"); // length 11

        // Act
        double effort = predictor.predict(thoughtToPredict);

        // Assert
        // Expected effort = 0.5 * 11 + 5.0 = 5.5 + 5.0 = 10.5
        assertEquals(10.5, effort, DELTA);
    }

    @Test
    void predict_whenSchemaNotFound_usesDefaultModel() {
        // Arrange
        when(mockMemoryNexus.findSchemaBySymbolicName(EffortPredictor.EFFORT_MODEL_SCHEMA_NAME))
                .thenReturn(Optional.empty());

        Thought thoughtToPredict = createTestThought("hello world"); // length 11

        // Act
        double effort = predictor.predict(thoughtToPredict);

        // Assert
        // Default model is (0.1 * length + 1.0)
        // Expected effort = 0.1 * 11 + 1.0 = 1.1 + 1.0 = 2.1
        double expected = LinearTextEffortModel.DEFAULT.predict(thoughtToPredict);
        assertEquals(expected, effort, DELTA);
    }

    @Test
    void predict_whenSchemaHasInvalidProceduralContent_usesDefaultModel() {
        // Arrange
        // Create a schema with a String in procedural content instead of a model
        Thought invalidSchema = createSchemaThought("not a model");
        when(mockMemoryNexus.findSchemaBySymbolicName(EffortPredictor.EFFORT_MODEL_SCHEMA_NAME))
                .thenReturn(Optional.of(invalidSchema));

        Thought thoughtToPredict = createTestThought("hello world");

        // Act
        double effort = predictor.predict(thoughtToPredict);

        // Assert
        double expected = LinearTextEffortModel.DEFAULT.predict(thoughtToPredict);
        assertEquals(expected, effort, DELTA);
    }

    private Thought createSchemaThought(Object proceduralContent) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Schema", EffortPredictor.EFFORT_MODEL_SCHEMA_NAME, null, null, proceduralContent, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMetadata(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );
    }
}
