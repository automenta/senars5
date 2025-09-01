package com.senars.cycle;

import com.senars.core.Thought;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An implementation of the Attention Funnel that selects the focus thought
 * based on the highest calculated salience score.
 */
public class SalienceBasedAttention implements Attention {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalienceBasedAttention.class);
    private final List<Thought> candidates = new CopyOnWriteArrayList<>();
    private final SalienceCalculator salienceCalculator;
    private final MotiveHierarchy motiveHierarchy;
    private final EventBus eventBus;

    public SalienceBasedAttention(SalienceCalculator salienceCalculator, MotiveHierarchy motiveHierarchy, EventBus eventBus) {
        this.salienceCalculator = salienceCalculator;
        this.motiveHierarchy = motiveHierarchy;
        this.eventBus = eventBus;
    }

    @Override
    public void addCandidate(Thought thought) {
        if (thought != null) {
            candidates.add(thought);
        }
    }

    // ...
    @Override
    public Optional<Thought> selectFocusThought() {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        LOGGER.debug("--- Attention Funnel: Selecting Focus Thought ---");
        List<Thought> currentCandidates = List.copyOf(candidates);

        // Calculate salience for all candidates and find the one with the max score
        Optional<Thought> bestThought = currentCandidates.stream()
                .map(thought -> {
                    double salience = salienceCalculator.calculate(thought, motiveHierarchy);
                    LOGGER.debug("Candidate: '{}' (ID: {}) - Calculated Salience: {}", thought.content().text(), thought.id(), String.format("%.4f", salience));
                    return new AbstractMap.SimpleImmutableEntry<>(thought, salience);
                })
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
}
