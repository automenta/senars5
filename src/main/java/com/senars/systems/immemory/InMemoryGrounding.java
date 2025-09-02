package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An in-memory implementation of the Grounding system.
 * It processes feedback from actions to adjust the clarity of thoughts in the
 * Memory Nexus, reinforcing or correcting the system's knowledge and creating
 * new goals to handle failures.
 */
public class InMemoryGrounding implements Grounding {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryGrounding.class);
    private static final double DEFAULT_REINFORCEMENT_FACTOR = 0.1; // Learn moderately from success
    private static final double DEFAULT_BLAME_FACTOR = 0.3;       // Learn aggressively from failure
    private static final double DECAY_FACTOR = 0.9; // How much less to blame/reward thoughts further up the trace

    private final Memory memory;
    private final EventBus eventBus;
    private final double reinforcementFactor;
    private final double blameFactor;


    public InMemoryGrounding(Memory memory, EventBus eventBus) {
        this(memory, eventBus, DEFAULT_REINFORCEMENT_FACTOR, DEFAULT_BLAME_FACTOR);
    }

    public InMemoryGrounding(Memory memory, EventBus eventBus, double reinforcementFactor, double blameFactor) {
        this.memory = Objects.requireNonNull(memory);
        this.eventBus = Objects.requireNonNull(eventBus);
        this.reinforcementFactor = reinforcementFactor;
        this.blameFactor = blameFactor;
    }


    @Override
    public void processFeedback(Feedback feedback) {
        if (feedback == null || feedback.actionPlan() == null) {
            LOGGER.warn("Received feedback with no action plan. Ignoring.");
            return;
        }

        Thought actionPlan = feedback.actionPlan();
        List<String> traceIds = actionPlan.metadata().trace();

        if (traceIds == null || traceIds.isEmpty()) {
            LOGGER.info("Feedback for action {} has no trace. No thoughts to adjust.", actionPlan.id());
            return;
        }

        double initialAdjustment = switch (feedback.status()) {
            case SUCCESS -> this.reinforcementFactor;
            case FAILURE -> -this.blameFactor;
        };

        LOGGER.info("Processing feedback for action {}. Status: {}. Adjusting clarity for {} thoughts with initial factor {}.",
                actionPlan.id(), feedback.status(), traceIds.size(), String.format("%.4f", initialAdjustment));

        // Adjust clarity of the action plan itself, as it's the most direct cause.
        adjustClarityOfSingleThought(actionPlan, initialAdjustment, 0);

        // Adjust clarity of the other thoughts in the provenance trace
        adjustClarityInTrace(traceIds, initialAdjustment, 1); // Start decay from 1 step away

        // If the action failed, create a new goal to investigate
        if (feedback.status() == ActionStatus.FAILURE) {
            createAndPublishFailureGoal(feedback);
        }
    }

    private void adjustClarityInTrace(List<String> traceIds, double initialAdjustment, int startDecay) {
        int traceSize = traceIds.size();
        for (int i = 0; i < traceSize; i++) {
            String thoughtId = traceIds.get(i);
            int decaySteps = startDecay + (traceSize - 1 - i);
            memory.getThoughtById(thoughtId).ifPresent(thoughtToUpdate ->
                    adjustClarityOfSingleThought(thoughtToUpdate, initialAdjustment, decaySteps));
        }
    }

    private void adjustClarityOfSingleThought(Thought thoughtToUpdate, double initialAdjustment, int decaySteps) {
        double decayedAdjustment = initialAdjustment * Math.pow(DECAY_FACTOR, decaySteps);

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
            LOGGER.debug("Updated clarity of thought {} (decay steps: {}) from {} to {} (adjustment: {})",
                    updatedThought.id(), decaySteps, String.format("%.4f", currentClarity), String.format("%.4f", newClarity), String.format("%.4f", decayedAdjustment));
        }
    }

    private void createAndPublishFailureGoal(Feedback feedback) {
        String goalText = String.format("Investigate and resolve failure of tool '%s'. Error: %s",
                feedback.toolName(), feedback.output());

        ThoughtContent content = new ThoughtContent(goalText, null, null, null, null, null, null);

        // This goal should trace back to the failed action plan
        List<String> trace = List.of(feedback.actionPlan().id());
        ThoughtMeta meta = new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.SYSTEM, trace, Instant.now());

        // Give it high salience to ensure it is picked up quickly
        ThoughtState state = new ThoughtState(1.0, 100.0, 1.0);

        Thought failureGoal = new Thought(UUID.randomUUID().toString(), content, state, meta);
        LOGGER.info("Generated new failure-driven goal: {}", failureGoal.id());

        // Publish the new goal to the event bus so the cognitive cycle can add it to the attention funnel
        eventBus.publish(new Events.NewThoughtCreatedEvent(failureGoal));
    }
}
