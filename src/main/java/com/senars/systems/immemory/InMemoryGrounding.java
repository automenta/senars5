package com.senars.systems.immemory;

import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.core.ThoughtState;
import com.senars.events.EventBus;
import com.senars.events.Events;
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
    private static final double DEFAULT_REINFORCEMENT_FACTOR = 0.1; // Learn moderately from success
    private static final double DEFAULT_BLAME_FACTOR = 0.2;       // Learn aggressively from failure

    private final Memory memory;
    private final EventBus eventBus;
    private final double reinforcementFactor;
    private final double blameFactor;
    private final double decayFactor = 0.9; // How much less to blame/reward thoughts further up the trace

    /**
     * Default constructor using default learning factors.
     * @param memory The memory system to update.
     */
    public InMemoryGrounding(Memory memory, EventBus eventBus) {
        this(memory, eventBus, DEFAULT_REINFORCEMENT_FACTOR, DEFAULT_BLAME_FACTOR);
    }

    /**
     * Constructor for symmetric learning.
     * @param memory The memory system to update.
     * @param clarityAdjustmentFactor The factor to use for both reinforcement and blame.
     */
    public InMemoryGrounding(Memory memory, EventBus eventBus, double clarityAdjustmentFactor) {
        this(memory, eventBus, clarityAdjustmentFactor, clarityAdjustmentFactor);
    }

    /**
     * Full constructor for asymmetric learning.
     * @param memory The memory system to update.
     * @param reinforcementFactor The factor for adjusting clarity on success (feedback > 0.5).
     * @param blameFactor The factor for adjusting clarity on failure (feedback < 0.5).
     */
    public InMemoryGrounding(Memory memory, EventBus eventBus, double reinforcementFactor, double blameFactor) {
        this.memory = Objects.requireNonNull(memory);
        this.eventBus = Objects.requireNonNull(eventBus);
        this.reinforcementFactor = reinforcementFactor;
        this.blameFactor = blameFactor;
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

        // Asymmetric adjustment: apply different factors for success and failure.
        double initialAdjustment;
        if (feedback.success() >= 0.5) {
            // Scale the success range [0.5, 1.0] to [0, 1] and apply reinforcement factor
            initialAdjustment = (feedback.success() - 0.5) * 2 * this.reinforcementFactor;
        } else {
            // Scale the failure range [0.0, 0.5) to [-1, 0) and apply blame factor
            initialAdjustment = (feedback.success() - 0.5) * 2 * this.blameFactor;
        }


        LOGGER.info("Processing feedback for report {}. Adjusting clarity for {} thoughts with initial adjustment {} and decay {}.",
                feedbackReport.id(), traceIds.size(), String.format("%.4f", initialAdjustment), decayFactor);

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
                    eventBus.publish(new Events.ClarityUpdatedEvent(updatedThought.id(), currentClarity, newClarity));
                    LOGGER.debug("Updated clarity of thought {} (distance from end: {}) from {} to {} (adjustment: {})",
                            updatedThought.id(), distanceFromEnd, String.format("%.4f", currentClarity), String.format("%.4f", newClarity), String.format("%.4f", decayedAdjustment));
                }
            });
        }
    }
}
