package com.senars.explain;

import com.senars.core.Thought;
import com.senars.systems.Memory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Explainable AI (XAI) Engine.
 * This class is responsible for reconstructing the reasoning process that led to a specific thought.
 */
public class Explain {

    private final Memory memory;

    public Explain(Memory memory) {
        this.memory = memory;
    }

    /**
     * Reconstructs the causal chain of thoughts that led to the target thought.
     * It returns the list of thoughts in the trace.
     *
     * @param targetThought The thought to be explained.
     * @return A list of thoughts representing the reasoning trace.
     */
    public List<Thought> getTrace(Thought targetThought) {
        if (targetThought == null || targetThought.metadata().trace() == null) {
            return new ArrayList<>();
        }

        return targetThought.metadata().trace().stream()
                .map(memory::getThoughtById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Formats a list of thoughts into a human-readable string for explanation.
     *
     * @param trace The list of thoughts in the causal chain.
     * @param targetThought The final thought that was the subject of the explanation.
     * @return A formatted string detailing the reasoning trace.
     */
    public String formatTrace(List<Thought> trace, Thought targetThought) {
        if (trace.isEmpty()) {
            return "The thought '" + targetThought.content().text() + "' has no recorded reasoning trace. It may be a foundational thought or user input.";
        }

        StringBuilder explanation = new StringBuilder();
        explanation.append("The reasoning for '").append(targetThought.content().text()).append("' was as follows:\n");

        for (int i = 0; i < trace.size(); i++) {
            Thought thought = trace.get(i);
            explanation.append(String.format("  %d. [%s] %s\n", i + 1, thought.metadata().type(), thought.content().text()));
        }

        explanation.append("Which led to the final conclusion.\n");
        return explanation.toString();
    }
}
