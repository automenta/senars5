package com.senars.attention;

import com.senars.core.Thought;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple implementation of the Attention Funnel that operates on a First-In, First-Out (FIFO) basis.
 * This implementation is useful for testing and for scenarios where complex salience calculation is not needed.
 */
public class FifoAttention implements Attention {

    private final Queue<Thought> candidates = new ConcurrentLinkedQueue<>();

    @Override
    public void addCandidate(Thought thought) {
        if (thought != null) {
            candidates.add(thought);
        }
    }

    @Override
    public Optional<Thought> selectFocusThought() {
        return Optional.ofNullable(candidates.poll());
    }
}
