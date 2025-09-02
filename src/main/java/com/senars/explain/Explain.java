package com.senars.explain;

import com.senars.core.CausalLink;
import com.senars.core.Thought;
import com.senars.systems.Memory;

import java.util.*;
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
     * It returns the list of thoughts in the causal chain.
     *
     * @param targetThought The thought to be explained.
     * @return A list of thoughts representing the causal chain.
     */
    public List<Thought> getCausalChain(Thought targetThought) {
        if (targetThought == null) {
            return new ArrayList<>();
        }

        // Build a map of thought IDs to thoughts for efficient lookup
        Map<String, Thought> thoughtMap = new HashMap<>();
        
        // Get all thoughts in the causal chain
        Set<String> thoughtIds = new HashSet<>();
        collectCausalThoughtIds(targetThought, thoughtIds);
        
        // Load all thoughts from memory
        for (String thoughtId : thoughtIds) {
            memory.getThoughtById(thoughtId).ifPresent(thought -> thoughtMap.put(thoughtId, thought));
        }
        
        // Build the causal chain ordered from root to target
        return buildCausalChain(targetThought, thoughtMap);
    }
    
    /**
     * Recursively collects all thought IDs in the causal chain.
     *
     * @param thought The thought to start from.
     * @param thoughtIds The set to collect thought IDs into.
     */
    private void collectCausalThoughtIds(Thought thought, Set<String> thoughtIds) {
        if (thought == null || thoughtIds.contains(thought.id())) {
            return;
        }
        
        thoughtIds.add(thought.id());
        
        if (thought.metadata().causalLinks() != null) {
            for (CausalLink link : thought.metadata().causalLinks()) {
                memory.getThoughtById(link.sourceThoughtId()).ifPresent(causeThought -> 
                    collectCausalThoughtIds(causeThought, thoughtIds));
            }
        }
    }
    
    /**
     * Builds the causal chain ordered from root to target.
     *
     * @param targetThought The target thought.
     * @param thoughtMap The map of thought IDs to thoughts.
     * @return A list of thoughts representing the causal chain.
     */
    private List<Thought> buildCausalChain(Thought targetThought, Map<String, Thought> thoughtMap) {
        List<Thought> chain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        buildCausalChainRecursive(targetThought, thoughtMap, chain, visited);
        chain.add(targetThought); // Add the target thought itself
        Collections.reverse(chain); // Reverse to get root-to-target order
        return chain;
    }
    
    /**
     * Recursively builds the causal chain.
     *
     * @param thought The current thought.
     * @param thoughtMap The map of thought IDs to thoughts.
     * @param chain The chain being built.
     * @param visited The set of visited thought IDs.
     */
    private void buildCausalChainRecursive(Thought thought, Map<String, Thought> thoughtMap, 
                                          List<Thought> chain, Set<String> visited) {
        if (thought == null || visited.contains(thought.id())) {
            return;
        }
        
        visited.add(thought.id());
        
        if (thought.metadata().causalLinks() != null) {
            for (CausalLink link : thought.metadata().causalLinks()) {
                Thought causeThought = thoughtMap.get(link.sourceThoughtId());
                if (causeThought != null) {
                    if (!visited.contains(causeThought.id())) {
                        chain.add(causeThought);
                        buildCausalChainRecursive(causeThought, thoughtMap, chain, visited);
                    }
                }
            }
        }
    }

    /**
     * Formats a list of thoughts into a human-readable string for explanation.
     *
     * @param causalChain The list of thoughts in the causal chain.
     * @param targetThought The final thought that was the subject of the explanation.
     * @return A formatted string detailing the causal chain.
     */
    public String formatCausalChain(List<Thought> causalChain, Thought targetThought) {
        if (causalChain.isEmpty()) {
            return "The thought '" + targetThought.content().text() + "' has no recorded causal chain. It may be a foundational thought or user input.";
        }

        StringBuilder explanation = new StringBuilder();
        explanation.append("The reasoning for '").append(targetThought.content().text()).append("' was as follows:\n");

        for (int i = 0; i < causalChain.size(); i++) {
            Thought thought = causalChain.get(i);
            explanation.append(String.format("  %d. [%s] %s\n", i + 1, thought.metadata().type(), thought.content().text()));
        }

        explanation.append("Which led to the final conclusion.\n");
        return explanation.toString();
    }
}
