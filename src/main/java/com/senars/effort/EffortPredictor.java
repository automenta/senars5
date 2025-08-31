package com.senars.effort;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.systems.IMemoryNexus;
import java.util.Optional;

/**
 * A service responsible for predicting the computational effort required to process a Thought.
 * It retrieves an effort prediction model from a SCHEMA in the Memory Nexus.
 */
public class EffortPredictor {

    public static final String EFFORT_MODEL_SCHEMA_NAME = "senars:effort_prediction_model_v1";
    private final IMemoryNexus memoryNexus;

    public EffortPredictor(IMemoryNexus memoryNexus) {
        this.memoryNexus = memoryNexus;
    }

    /**
     * Predicts the effort for a given Thought.
     * It searches for a specific SCHEMA in the memory nexus that contains the prediction model.
     * If no model is found, it falls back to a default model.
     *
     * @param thought The Thought to predict effort for.
     * @return The predicted effort.
     */
    public double predict(Thought thought) {
        Optional<Thought> modelSchema = memoryNexus.findSchemaBySymbolicName(EFFORT_MODEL_SCHEMA_NAME);

        if (modelSchema.isPresent()) {
            Thought schema = modelSchema.get();
            if (schema.metadata().type() == ThoughtType.SCHEMA && schema.content().procedural() instanceof IEffortPredictionModel) {
                IEffortPredictionModel model = (IEffortPredictionModel) schema.content().procedural();
                return model.predict(thought);
            }
        }

        // Fallback to a default model if no valid schema is found
        return LinearTextEffortModel.DEFAULT.predict(thought);
    }
}
