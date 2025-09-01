package com.senars.cycle;

import com.senars.core.Thought;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A thread-safe queue to hold feedback from executed actions.
 * The Action system places REPORT thoughts here, and the Perception system consumes them.
 */
public class ActionFeedbackQueue {

    private final BlockingQueue<Thought> queue = new LinkedBlockingQueue<>();

    public void add(Thought thought) {
        queue.add(thought);
    }

    public Thought poll() {
        return queue.poll();
    }
}
