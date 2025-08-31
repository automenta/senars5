package com.senars.cycle;

import com.senars.core.Thought;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;

import java.util.Comparator;
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

    @Override
    public Optional<Thought> selectFocusThought() {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Find the thought with the maximum salience score
        Optional<Thought> bestThought = candidates.stream()
                .max(Comparator.comparingDouble(thought -> salienceCalculator.calculate(thought, motiveHierarchy)));

        // Remove the selected thought from the candidates list
        bestThought.ifPresent(candidates::remove);

        return bestThought;
    }
}
