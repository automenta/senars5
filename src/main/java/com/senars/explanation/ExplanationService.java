package com.senars.explanation;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.events.EventBus;
import com.senars.explain.Explain;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Memory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The Explanation Service builds an interface that exposes parts of the Cognitive Kernel's API.
 * It handles adaptive explanation and multi-agent negotiation.
 */
public class ExplanationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExplanationService.class);

    private final Memory memory;
    private final UnifiedCausalReasoner ucr;
    private final EventBus eventBus;
    private final ChatLanguageModel chatModel;
    private final Explain explain;

    public ExplanationService(Memory memory, UnifiedCausalReasoner ucr, EventBus eventBus, ChatLanguageModel chatModel) {
        this.memory = memory;
        this.ucr = ucr;
        this.eventBus = eventBus;
        this.chatModel = chatModel;
        this.explain = new Explain(memory);
    }

    /**
     * Creates an adaptive explanation for a given thought in response to a user's "why?" question.
     *
     * @param targetThoughtId The ID of the thought to explain
     * @param userQuestion The user's question (optional)
     * @return A CompletableFuture containing the explanation text
     */
    public CompletableFuture<String> createAdaptiveExplanation(String targetThoughtId, String userQuestion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get the target thought
                Optional<Thought> targetThoughtOpt = memory.getThoughtById(targetThoughtId);
                if (targetThoughtOpt.isEmpty()) {
                    return "Could not find the requested thought in memory.";
                }

                Thought targetThought = targetThoughtOpt.get();

                // Get the causal chain
                List<Thought> causalChain = explain.getCausalChain(targetThought);

                // Format the explanation
                String basicExplanation = explain.formatCausalChain(causalChain, targetThought);

                // If we have a user question, generate a more focused explanation
                if (userQuestion != null && !userQuestion.trim().isEmpty()) {
                    return generateFocusedExplanation(basicExplanation, userQuestion);
                }

                return basicExplanation;
            } catch (Exception e) {
                LOGGER.error("Error creating adaptive explanation", e);
                return "Error generating explanation: " + e.getMessage();
            }
        });
    }

    /**
     * Analyzes a proposed plan from another agent for conflict analysis.
     *
     * @param proposedPlan The proposed plan from another agent
     * @return A CompletableFuture containing the conflict analysis
     */
    public CompletableFuture<String> analyzeAgentPlan(Thought proposedPlan) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Analyzing proposed plan from another agent");

                // Simulate the proposed plan using the UCR
                List<Thought> simulationResults = ucr.reason(
                        proposedPlan,
                        "forward",
                        new UnifiedCausalReasoner.ReasoningOptions(true, 5)
                );

                // Check for conflicts with existing knowledge or safety constraints
                StringBuilder analysis = new StringBuilder();
                analysis.append("Analysis of proposed plan: ").append(proposedPlan.content().text()).append("\n");

                boolean hasConflicts = false;

                // Check for conflicts with existing beliefs
                for (Thought result : simulationResults) {
                    if (result.metadata().type() == ThoughtType.REPORT) {
                        // Look for conflict indicators in the simulation results
                        if (result.content().text().contains("conflict") ||
                                result.content().text().contains("inconsistent") ||
                                result.content().text().contains("contradiction")) {
                            analysis.append("Potential conflict detected: ").append(result.content().text()).append("\n");
                            hasConflicts = true;
                        }
                    }
                }

                if (!hasConflicts) {
                    analysis.append("No immediate conflicts detected with existing knowledge.\n");
                }

                // Add safety analysis
                analysis.append("Safety analysis: ");
                String safetyAnalysis = performSafetyAnalysis(proposedPlan);
                analysis.append(safetyAnalysis);

                return analysis.toString();
            } catch (Exception e) {
                LOGGER.error("Error analyzing agent plan", e);
                return "Error analyzing proposed plan: " + e.getMessage();
            }
        });
    }

    /**
     * Generates a more focused explanation based on the user's specific question.
     *
     * @param basicExplanation The basic causal chain explanation
     * @param userQuestion The user's question
     * @return A focused explanation
     */
    private String generateFocusedExplanation(String basicExplanation, String userQuestion) {
        try {
            String prompt = String.format(
                    """
                            You are an AI assistant that explains complex reasoning processes. \
                            A user has asked a specific question about a reasoning process. \
                            Please provide a focused explanation that directly addresses their question.
                            
                            Basic explanation:
                            %s
                            
                            User's question: %s
                            
                            Please provide a concise, focused explanation that directly answers the user's question.""",
                    basicExplanation,
                    userQuestion
            );

            return chatModel.generate(UserMessage.from(prompt)).content().text();
        } catch (Exception e) {
            LOGGER.error("Error generating focused explanation", e);
            return "Error generating focused explanation. Here's the basic explanation:\n\n" + basicExplanation;
        }
    }

    /**
     * Performs a safety analysis of a proposed action or plan.
     *
     * @param actionOrPlan The action or plan to analyze
     * @return A safety analysis report
     */
    private String performSafetyAnalysis(Thought actionOrPlan) {
        try {
            // This would typically integrate with the Governance Service
            // For now, we'll provide a placeholder implementation

            // Simple heuristics for safety analysis
            String actionText = actionOrPlan.content().text() != null ? actionOrPlan.content().text().toLowerCase() : "";

            if (actionText.contains("delete") || actionText.contains("remove") || actionText.contains("destroy")) {
                return "CAUTION: Action involves deletion/removal operations. Please verify this is intended.";
            }

            if (actionText.contains("access") && (actionText.contains("password") || actionText.contains("secret"))) {
                return "WARNING: Action involves accessing sensitive information. Verify authorization.";
            }

            return "No immediate safety concerns identified.";
        } catch (Exception e) {
            LOGGER.error("Error performing safety analysis", e);
            return "Error performing safety analysis.";
        }
    }

    /**
     * Handles a follow-up question from a user about an existing explanation.
     *
     * @param originalExplanation The original explanation
     * @param followUpQuestion The follow-up question
     * @return A response to the follow-up question
     */
    public CompletableFuture<String> handleFollowUpQuestion(String originalExplanation, String followUpQuestion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = String.format(
                        """
                                You are an AI assistant that explains complex reasoning processes. \
                                A user has a follow-up question about a previous explanation. \
                                Please provide a clear, concise response to their question.
                                
                                Original explanation:
                                %s
                                
                                Follow-up question: %s
                                
                                Please provide a response that builds on the original explanation.""",
                        originalExplanation,
                        followUpQuestion
                );

                return chatModel.generate(UserMessage.from(prompt)).content().text();
            } catch (Exception e) {
                LOGGER.error("Error handling follow-up question", e);
                return "Error processing follow-up question: " + e.getMessage();
            }
        });
    }
}