package com.senars.cycle;

import com.senars.core.Thought;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A concrete implementation of the Attention Funnel that prioritizes thoughts
 * based on their calculated salience.
 */
public class SalienceAttention implements Attention {

    private final List<Thought> candidates = new CopyOnWriteArrayList<>();
    private final SalienceCalculator salienceCalculator;
    private final MotiveHierarchy motiveHierarchy;

    /**
     * Constructs an AttentionFunnel.
     *
     * @param salienceCalculator The calculator used to determine thought salience.
     * @param motiveHierarchy    The motive hierarchy used for salience calculation.
     */
    public SalienceAttention(SalienceCalculator salienceCalculator, MotiveHierarchy motiveHierarchy) {
        this.salienceCalculator = Objects.requireNonNull(salienceCalculator);
        this.motiveHierarchy = Objects.requireNonNull(motiveHierarchy);
    }

    @Override
    public void addCandidate(Thought thought) {
        // Avoid adding duplicates
        if (!candidates.contains(thought)) {
            candidates.add(thought);
        }
    }

    @Override
    public Optional<Thought> selectFocusThought() {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Optional<Thought> focusThought = candidates.stream()
                .max(Comparator.comparingDouble(thought -> salienceCalculator.calculate(thought, motiveHierarchy)));

        focusThought.ifPresent(candidates::remove);

        return focusThought;
    }
}
