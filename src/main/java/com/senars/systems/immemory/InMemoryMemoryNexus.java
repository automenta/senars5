package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.salience.VectorMath;
import com.senars.systems.IMemoryNexus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An in-memory implementation of the IMemoryNexus interface.
 * Suitable for testing and development without requiring external databases.
 * Note: The semantic search is a naive O(n) implementation.
 */
public class InMemoryMemoryNexus implements IMemoryNexus {

    private final Map<String, Thought> thoughtStore = new ConcurrentHashMap<>();

    @Override
    public void saveThought(Thought thought) {
        thoughtStore.put(thought.id(), thought);
    }

    @Override
    public Optional<Thought> getThoughtById(String id) {
        return Optional.ofNullable(thoughtStore.get(id));
    }

    @Override
    public List<Thought> retrieveSimilar(List<Double> embedding, int topK) {
        if (embedding == null || embedding.isEmpty()) {
            return Collections.emptyList();
        }

        // Naive O(n) semantic search.
        return thoughtStore.values().stream()
            .filter(thought -> thought.content().embedding() != null && !thought.content().embedding().isEmpty())
            .map(thought -> {
                double similarity = VectorMath.cosineSimilarity(embedding, thought.content().embedding());
                return new AbstractMap.SimpleEntry<>(thought, similarity);
            })
            .sorted(Map.Entry.<Thought, Double>comparingByValue().reversed())
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public List<Thought> getTrace(String thoughtId) {
        List<Thought> trace = new ArrayList<>();
        Optional<Thought> currentThoughtOpt = getThoughtById(thoughtId);

        while (currentThoughtOpt.isPresent()) {
            Thought currentThought = currentThoughtOpt.get();
            trace.add(currentThought);

            List<String> parentIds = currentThought.metadata().trace();
            if (parentIds == null || parentIds.isEmpty()) {
                break;
            }

            // For simplicity in this mock, we only trace back the first parent.
            // A full graph implementation would handle multiple parents.
            currentThoughtOpt = getThoughtById(parentIds.get(0));
        }

        Collections.reverse(trace); // To get the trace in chronological order.
        return trace;
    }
}
