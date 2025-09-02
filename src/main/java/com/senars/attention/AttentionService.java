package com.senars.attention;

import com.senars.core.Thought;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.motive.MotiveHierarchy;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The unified Attention Service for the SeNARS Cognitive Architecture.
 * This service implements the Attention interface and is the central point for managing
 * candidate thoughts and selecting the most salient one for processing.
 * It uses a SalienceCalculator and the UCR for intelligent effort estimation to prioritize thoughts.
 */
public class AttentionService implements Attention {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttentionService.class);

    private final List<Thought> candidates = new CopyOnWriteArrayList<>();
    private final SalienceCalculator salienceCalculator;
    private final MotiveHierarchy motiveHierarchy;
    private final EventBus eventBus;
    private final UnifiedCausalReasoner ucr;
    private final Memory memory;

    public AttentionService(Memory memory, SalienceCalculator salienceCalculator, UnifiedCausalReasoner ucr, MotiveHierarchy motiveHierarchy, EventBus eventBus) {
        this.memory = memory;
        this.salienceCalculator = salienceCalculator;
        this.ucr = ucr;
        this.motiveHierarchy = motiveHierarchy;
        this.eventBus = eventBus;
    }

    @Override
    public void addCandidate(Thought thought) {
        if (thought != null && !candidates.contains(thought)) {
            candidates.add(thought);
        }
    }

    @Override
    public Optional<Thought> selectFocusThought() {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        LOGGER.debug("--- Attention Funnel: Selecting Focus Thought from {} candidates ---", candidates.size());

        Optional<Thought> bestThought = candidates.stream()
                .map(thought -> new AbstractMap.SimpleImmutableEntry<>(thought, calculateIntelligentSalience(thought)))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        bestThought.ifPresent(thought -> {
            LOGGER.info("Selected Focus Thought: '{}' (ID: {})", thought.content().text(), thought.id());
            eventBus.publish(new Events.FocusThoughtSelectedEvent(thought));
            candidates.remove(thought);
        });
        LOGGER.debug("-------------------------------------------------");

        return bestThought;
    }

    private double calculateIntelligentSalience(Thought thought) {
        // First, get the base salience using the existing calculator
        double baseSalience = salienceCalculator.calculate(thought, motiveHierarchy);
        double finalSalience = baseSalience;

        // Then, try to get a more accurate effort estimation from the UCR
        try {
            // Use a lightweight estimation mode of the UCR.
            List<Thought> estimationResults = ucr.reason(thought, "forward", UnifiedCausalReasoner.ReasoningOptions.estimation());

            if (!estimationResults.isEmpty()) {
                Thought estimationResult = estimationResults.getFirst();
                String estimationText = estimationResult.content().text();

                if (estimationText != null) {
                    // This parsing logic is a placeholder for a more robust mechanism.
                    String[] parts = estimationText.split(": ");
                    if (parts.length >= 3) {
                        double predictedEffort = Double.parseDouble(parts[2].split(" ")[0]);
                        if (predictedEffort > 0) {
                            // Adjust salience based on the more accurate effort.
                            finalSalience = baseSalience / predictedEffort;
                            LOGGER.trace("Adjusted salience for thought {} from {} to {} based on UCR effort estimation.", thought.id(), baseSalience, finalSalience);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error during UCR effort estimation for thought {}. Using base salience. Error: {}", thought.id(), e.getMessage());
        }

        LOGGER.debug("Candidate: '{}' (ID: {}) - Calculated Salience: {}", thought.content().text(), thought.id(), String.format("%.4f", finalSalience));
        return finalSalience;
    }
}
