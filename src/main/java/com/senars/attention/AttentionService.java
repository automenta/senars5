package com.senars.attention;

import com.senars.core.Thought;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Memory;
import com.senars.motive.MotiveHierarchy;
import com.senars.systems.ScoredThought;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * An intelligent Attention Service that prioritizes Thoughts by calling the UCR
 * in a lightweight estimation mode to get an accurate PredictedEffort.
 * This makes the entire system more efficient by ensuring cognitive resources
 * are spent on the most promising tasks first.
 */
public class AttentionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttentionService.class);
    
    private final Memory memory;
    private final SalienceCalculator salienceCalculator;
    private final UnifiedCausalReasoner ucr;
    private final MotiveHierarchy motiveHierarchy;
    
    public AttentionService(Memory memory, SalienceCalculator salienceCalculator, 
                          UnifiedCausalReasoner ucr, MotiveHierarchy motiveHierarchy) {
        this.memory = memory;
        this.salienceCalculator = salienceCalculator;
        this.ucr = ucr;
        this.motiveHierarchy = motiveHierarchy;
    }
    
    /**
     * Selects the most salient Thought from a list of candidates using intelligent prioritization.
     * 
     * @param candidateThoughts List of candidate thoughts to evaluate
     * @return The most salient thought, or empty if no candidates
     */
    public Optional<Thought> selectFocusThought(List<Thought> candidateThoughts) {
        if (candidateThoughts.isEmpty()) {
            return Optional.empty();
        }
        
        // Calculate intelligent salience scores for all candidates
        List<ScoredThought> scoredThoughts = candidateThoughts.stream()
            .map(thought -> {
                double salience = calculateIntelligentSalience(thought);
                return new ScoredThought(thought, salience);
            })
            .sorted((a, b) -> Double.compare(b.score(), a.score())) // Descending order
            .collect(Collectors.toList());
        
        LOGGER.info("Ranked {} candidate thoughts by salience", scoredThoughts.size());
        
        // Return the highest scoring thought
        return Optional.of(scoredThoughts.get(0).thought());
    }
    
    /**
     * Calculates an intelligent salience score for a thought using the UCR for effort estimation.
     * 
     * @param thought The thought to calculate salience for
     * @return The calculated salience score
     */
    private double calculateIntelligentSalience(Thought thought) {
        // First, get the base salience using the existing calculator
        double baseSalience = salienceCalculator.calculate(thought, motiveHierarchy);
        
        // Then, get a more accurate effort estimation from the UCR
        try {
            UnifiedCausalReasoner.ReasoningOptions options = 
                UnifiedCausalReasoner.ReasoningOptions.estimation();
            
            List<Thought> estimationResults = ucr.reason(thought, "forward", options);
            
            if (!estimationResults.isEmpty()) {
                // Extract the predicted effort from the estimation result
                Thought estimationResult = estimationResults.get(0);
                String estimationText = estimationResult.content().text();
                
                if (estimationText != null) {
                    // Parse the effort estimation from the text
                    // Format: "Effort estimation for thought [id]: [value] units"
                    String[] parts = estimationText.split(": ");
                    if (parts.length >= 3) {
                        try {
                            double predictedEffort = Double.parseDouble(parts[2].split(" ")[0]);
                            
                            // Adjust the salience based on the more accurate effort estimation
                            // Salience = BaseSalience / PredictedEffort
                            if (predictedEffort > 0) {
                                return baseSalience / predictedEffort;
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Could not parse effort estimation from text: {}", estimationText);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error during UCR effort estimation for thought {}, using base salience: {}", 
                       thought.id(), e.getMessage());
        }
        
        // Fall back to base salience if UCR estimation fails
        return baseSalience;
    }
    
    /**
     * Asynchronously selects the most salient Thought from a list of candidates.
     * 
     * @param candidateThoughts List of candidate thoughts to evaluate
     * @return A CompletableFuture that will contain the most salient thought
     */
    public CompletableFuture<Optional<Thought>> selectFocusThoughtAsync(List<Thought> candidateThoughts) {
        return CompletableFuture.supplyAsync(() -> selectFocusThought(candidateThoughts));
    }
}