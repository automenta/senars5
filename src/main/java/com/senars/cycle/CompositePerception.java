package com.senars.cycle;

import com.senars.core.Thought;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A concrete implementation of the Perception interface that manages multiple PerceptionChannel objects.
 * This class allows the cognitive system to gather inputs from various sources (e.g., console, files, web)
 * in a single perception stage of the cognitive cycle.
 */
public class CompositePerception implements Perception {

    private final List<PerceptionChannel> channels;

    /**
     * Constructs a CompositePerception with a list of channels.
     *
     * @param channels A list of PerceptionChannel objects to be managed. Must not be null.
     */
    public CompositePerception(List<PerceptionChannel> channels) {
        this.channels = Objects.requireNonNull(channels, "Channels list cannot be null");
    }

    /**
     * Perceives thoughts by iterating through all registered channels and aggregating their results.
     *
     * @return A single list containing all thoughts perceived from all channels in this cycle.
     */
    @Override
    public List<Thought> perceive() {
        List<Thought> allPerceivedThoughts = new ArrayList<>();
        for (PerceptionChannel channel : channels) {
            try {
                List<Thought> perceivedThoughts = channel.perceive();
                if (perceivedThoughts != null && !perceivedThoughts.isEmpty()) {
                    allPerceivedThoughts.addAll(perceivedThoughts);
                }
            } catch (Exception e) {
                // Log the exception but continue processing other channels
                // In a real system, you'd use a proper logger.
                System.err.println("Error perceiving from channel " + channel.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        return allPerceivedThoughts;
    }
}
