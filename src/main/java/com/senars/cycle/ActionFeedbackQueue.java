package com.senars.cycle;

import com.senars.core.Feedback;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A thread-safe queue to hold raw feedback from executed actions.
 * The CognitiveCycle places Feedback objects here, and consumes them to pass to the Grounding system.
 */
public class ActionFeedbackQueue {

    private final BlockingQueue<Feedback> queue = new LinkedBlockingQueue<>();

    public void add(Feedback feedback) {
        queue.add(feedback);
    }

    public Feedback poll() {
        return queue.poll();
    }
}
