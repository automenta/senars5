package com.senars.systems.immemory;

import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.core.ThoughtState;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * An in-memory implementation of the IGroundingSystem.
 * It processes feedback reports to adjust the clarity of thoughts in the
 * Memory Nexus, reinforcing or correcting the system's knowledge based on
 * action outcomes.
 */
public class InMemoryGrounding implements Grounding {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryGrounding.class);
    private static final double DEFAULT_CLARITY_ADJUSTMENT_FACTOR = 0.1;

    private final Memory memory;
    private final double clarityAdjustmentFactor;
    private final double decayFactor = 0.9;

    public InMemoryGrounding(Memory memory) {
        this(memory, DEFAULT_CLARITY_ADJUSTMENT_FACTOR);
    }

    public InMemoryGrounding(Memory memory, double clarityAdjustmentFactor) {
        this.memory = Objects.requireNonNull(memory);
        this.clarityAdjustmentFactor = clarityAdjustmentFactor;
    }

    @Override
    public void processFeedback(Thought feedbackReport) {
        if (feedbackReport == null || feedbackReport.content() == null || feedbackReport.content().feedback() == null) {
            LOGGER.warn("Received feedback report with no feedback content. Ignoring.");
            return;
        }

        Feedback feedback = feedbackReport.content().feedback();
        List<String> traceIds = feedbackReport.metadata().trace();

        if (traceIds == null || traceIds.isEmpty()) {
            LOGGER.info("Feedback report {} has no trace. No thoughts to adjust.", feedbackReport.id());
            return;
        }

        // The success metric determines the direction of the adjustment.
        // Success > 0.5 reinforces (increases clarity), < 0.5 blames (decreases clarity).
        double initialAdjustment = (feedback.success() - 0.5) * clarityAdjustmentFactor;

        LOGGER.info("Processing feedback for report {}. Adjusting clarity for {} thoughts with initial factor {} and decay {}.",
                feedbackReport.id(), traceIds.size(), initialAdjustment, decayFactor);

        int traceSize = traceIds.size();
        for (int i = 0; i < traceSize; i++) {
            String thoughtId = traceIds.get(i);
            // The last thought in the trace is the most recent, so it gets the highest adjustment.
            int distanceFromEnd = traceSize - 1 - i;
            double decayedAdjustment = initialAdjustment * Math.pow(decayFactor, distanceFromEnd);

            memory.getThoughtById(thoughtId).ifPresent(thoughtToUpdate -> {
                double currentClarity = thoughtToUpdate.state().clarity();
                double newClarity = Math.max(0.0, Math.min(1.0, currentClarity + decayedAdjustment));

                if (Math.abs(newClarity - currentClarity) > 1e-9) {
                    Thought updatedThought = new Thought(
                            thoughtToUpdate.id(),
                            thoughtToUpdate.content(),
                            new ThoughtState(newClarity, thoughtToUpdate.state().salience(), thoughtToUpdate.state().activation()),
                            thoughtToUpdate.metadata()
                    );
                    memory.saveThought(updatedThought);
                    LOGGER.debug("Updated clarity of thought {} (distance from end: {}) from {} to {} (adjustment: {})",
                            updatedThought.id(), distanceFromEnd, String.format("%.4f", currentClarity), String.format("%.4f", newClarity), String.format("%.4f", decayedAdjustment));
                }
            });
        }
    }
}
