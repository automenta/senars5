package com.senars.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.senars.core.*;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A service dedicated to meta-cognitive analysis, allowing the system to reflect
 * on its own state and behavior to overcome issues like cognitive loops or errors.
 */
public class MetaCognitiveService {

    private final ChatLanguageModel languageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetaCognitiveService(ChatLanguageModel languageModel) {
        this.languageModel = languageModel;
    }

    public CompletableFuture<Thought> analyzeCognitiveStall(List<Thought> history) {
        return CompletableFuture.supplyAsync(() -> {
            String historyText = history.stream()
                    .map(Thought::id)
                    .collect(Collectors.joining(", "));

            String promptTemplate = "You are a meta-cognitive analysis agent. The system you are in appears to be stuck in a cognitive loop, repeatedly processing the same thoughts. Your task is to analyze the history of recently focused thoughts and devise a new, concrete goal to break the cycle. \n\n**Recent Focus History (Thought IDs):**\n%s\n\n**Instructions:**\n1.  **Analyze the pattern:** Briefly describe the likely reason for the loop based on the thought history provided.\n2.  **Formulate a new goal:** Propose a single, specific, and actionable new GOAL to pursue. The goal should be designed to gather new information or take a different approach to the stalemate.\n3.  **Output Format:** Respond with a JSON object containing your analysis and the new goal, like this:\n```json\n{\n  \"analysis\": \"The system appears to be stuck trying to...\",\n  \"new_goal\": \"...\"\n}\n```";
            String prompt = String.format(promptTemplate, historyText);
            String response = languageModel.generate(UserMessage.from(prompt)).content().text();

            try {
                JsonNode responseNode = objectMapper.readTree(response.replace("```json", "").replace("```", ""));
                String newGoalText = responseNode.get("new_goal").asText();
                return createMetaGoal(newGoalText, "system_unstuck_resolution");
            } catch (Exception e) {
                // Failed to parse, return a fallback goal
                return createMetaGoal("The system is stuck. Formulate a plan to get unstuck.", "fallback_unstuck");
            }
        });
    }

    public CompletableFuture<Thought> analyzeSystemError(Throwable error) {
        return CompletableFuture.supplyAsync(() -> {
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();
            String errorDetails = String.format("Error: %s\nStackTrace (first 500 chars):\n%s", error.getMessage(), stackTrace.substring(0, Math.min(500, stackTrace.length())));

            String promptTemplate = "You are a system reliability engineer. A critical, unhandled exception has occurred during the cognitive cycle. Your task is to analyze the error and propose a course of action. \n\n**Error Details:**\n%s\n\n**Instructions:**\n1.  **Analyze the error:** Based on the error message and stack trace, diagnose the most likely root cause.\n2.  **Propose a recovery goal:** Formulate a single, specific, and actionable new GOAL to attempt to recover or gather more information. This could involve querying existing beliefs, inspecting a file, or asking the user for help. Do NOT propose to modify code or restart the system.\n3.  **Output Format:** Respond with a JSON object containing your analysis and the new goal, like this:\n```json\n{\n  \"analysis\": \"The error seems to be caused by...\",\n  \"recovery_goal\": \"...\"\n}\n```";
            String prompt = String.format(promptTemplate, errorDetails);
            String response = languageModel.generate(UserMessage.from(prompt)).content().text();

            try {
                JsonNode responseNode = objectMapper.readTree(response.replace("```json", "").replace("```", ""));
                String recoveryGoalText = responseNode.get("recovery_goal").asText();
                return createMetaGoal(recoveryGoalText, "system_error_recovery");
            } catch (Exception e) {
                return createMetaGoal("A system error occurred. Formulate a plan to recover.", "fallback_error");
            }
        });
    }

    private Thought createMetaGoal(String goalText, String symbolic) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(goalText, symbolic, null, null, null, null, null),
                new ThoughtState(1.0, 999.0, 1.0), // Max salience
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.META_COGNITION,
                        Collections.emptyList(),
                        Instant.now()
                )
        );
    }
}
