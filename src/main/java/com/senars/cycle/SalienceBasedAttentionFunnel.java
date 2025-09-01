package com.senars.cycle;

import com.senars.core.Thought;
import com.senars.core.Thought;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An implementation of the Attention Funnel that selects the focus thought
 * based on the highest calculated salience score.
 */
public class SalienceBasedAttentionFunnel implements IAttentionFunnel {

    private final List<Thought> candidates = new CopyOnWriteArrayList<>();
    private final SalienceCalculator salienceCalculator;
    private final MotiveHierarchy motiveHierarchy;

    public SalienceBasedAttentionFunnel(SalienceCalculator salienceCalculator, MotiveHierarchy motiveHierarchy) {
        this.salienceCalculator = salienceCalculator;
        this.motiveHierarchy = motiveHierarchy;
    }

    @Override
    public void addCandidate(Thought thought) {
        if (thought != null) {
            candidates.add(thought);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SalienceBasedAttentionFunnel.class);
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
                .peek(thought -> {
                    double salience = salienceCalculator.calculate(thought, motiveHierarchy);
                    LOGGER.debug("Candidate: '{}' (ID: {}) - Calculated Salience: {}", thought.content().text(), thought.id(), String.format("%.4f", salience));
                })
                .max(Comparator.comparingDouble(thought -> salienceCalculator.calculate(thought, motiveHierarchy)));

        bestThought.ifPresent(thought -> {
            LOGGER.info("Selected Focus Thought: '{}' (ID: {})", thought.content().text(), thought.id());
            candidates.remove(thought);
        });
        LOGGER.debug("-------------------------------------------------");


        return bestThought;
    }
}
