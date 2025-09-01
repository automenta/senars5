package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.cycle.Perception;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An implementation of the IPerceptionSystem that reads user input from the console.
 * It parses the input to create different types of Thoughts and generates embeddings for them.
 */
public class ConsolePerception implements Perception {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsolePerception.class);
    private final Scanner scanner;
    private final EmbeddingModel embeddingModel;

    public ConsolePerception(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public List<Thought> perceive() {
        System.out.print("> "); // Prompt for user input
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            // Handle special commands before attempting to parse as a Thought
            if (handleSpecialCommands(input)) {
                return Collections.emptyList(); // Command was handled, no thought produced
            }

            try {
                if (input.toLowerCase().startsWith("goal:")) {
                    return Collections.singletonList(createGoal(input.substring(5).trim()));
                } else if (input.toLowerCase().startsWith("belief:")) {
                    return Collections.singletonList(createBelief(input.substring(7).trim()));
                } else if (input.toLowerCase().startsWith("question:")) {
                    return Collections.singletonList(createQuestion(input.substring(9).trim()));
                } else if (input.toLowerCase().startsWith("feedback:")) {
                    return Collections.singletonList(createFeedbackReport(input.substring(9).trim()));
                } else if (input.toLowerCase().startsWith("why")) {
                    return Collections.singletonList(createExplanationRequest(input));
                } else if (!input.isEmpty()) {
                    // Default to creating a belief if no prefix is provided
                    return Collections.singletonList(createBelief(input));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to create thought from input: '{}'", input, e);
            }
        }
        return Collections.emptyList();
    }

    private boolean handleSpecialCommands(String input) {
        String command = input.toLowerCase();
        return switch (command) {
            case "shutdown", "exit" -> throw new ShutdownException();
            case "help" -> {
                printHelp();
                yield true;
            }
            default -> false;
        };
    }

    private void printHelp() {
        System.out.println("\n--- SeNARS Console Help ---");
        System.out.println("Create thoughts by typing a prefix followed by your text:");
        System.out.println("  goal: <your goal>       - Create a new goal for the system.");
        System.out.println("  belief: <a fact>        - Add a new belief to the system's memory.");
        System.out.println("  question: <your query>  - Ask a question.");
        System.out.println("  feedback: <0.0-1.0>     - Provide a score for the last action's outcome.");
        System.out.println("\nIf you don't provide a prefix, the input will be treated as a belief.");
        System.out.println("\nSpecial Commands:");
        System.out.println("  why                     - Explain the reasoning for the last action.");
        System.out.println("  why: <thought_id>       - Explain the reasoning for a specific thought.");
        System.out.println("  help                    - Display this help message.");
        System.out.println("  shutdown / exit         - Terminate the application.");
        System.out.println("---------------------------\n");
    }

    private Thought createExplanationRequest(String input) {
        String targetId;
        String[] parts = input.split(":", 2);
        if (parts.length > 1 && !parts[1].isBlank()) {
            targetId = parts[1].trim();
        } else {
            targetId = "last_action"; // Special keyword for the cognitive processor
        }

        ThoughtContent content = new ThoughtContent(targetId, null, null, null, null, null);
        ThoughtMeta metadata = new ThoughtMeta(ThoughtType.EXPLANATION_REQUEST, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
        ThoughtState state = new ThoughtState(1.0, 100.0, 1.0); // High salience to ensure it's processed
        return new Thought(UUID.randomUUID().toString(), content, state, metadata);
    }

    private List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Embedding embedding = embeddingModel.embed(text).content();
        // LangChain4J uses Float, but our Thought model uses Double for broader compatibility.
        return embedding.vectorAsList().stream().map(Float::doubleValue).collect(Collectors.toList());
    }

    private Thought createBelief(String text) {
        return createThought(text, ThoughtType.BELIEF);
    }

    private Thought createGoal(String text) {
        return createThought(text, ThoughtType.GOAL);
    }

    private Thought createQuestion(String text) {
        return createThought(text, ThoughtType.QUESTION);
    }

    private Thought createThought(String text, ThoughtType type) {
        List<Double> embedding = generateEmbedding(text);
        ThoughtContent content = new ThoughtContent(text, null, embedding, null, null, null);
        ThoughtMeta metadata = new ThoughtMeta(type, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
        ThoughtState state = new ThoughtState(0.9, 1.0, 1.0); // High clarity/salience for user input
        return new Thought(UUID.randomUUID().toString(), content, state, metadata);
    }

    private Thought createFeedbackReport(String feedbackInput) {
        try {
            double score = Double.parseDouble(feedbackInput);
            score = Math.max(0.0, Math.min(1.0, score)); // Clamp score to [0, 1]

            Feedback feedback = new Feedback(score, "User console feedback.");
            ThoughtContent content = new ThoughtContent(
                    "User feedback report. Score: " + score,
                    null, null, null, null, feedback);

            // Note: The trace for this feedback will need to be added by the component that manages the session,
            // as the perception system itself doesn't know which action this feedback is for.
            ThoughtMeta metadata = new ThoughtMeta(ThoughtType.REPORT, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
            ThoughtState state = new ThoughtState(1.0, 1.0, 1.0); // Feedback is always high clarity
            return new Thought(UUID.randomUUID().toString(), content, state, metadata);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid feedback score format: '{}'. Must be a number.", feedbackInput);
            // Return a special error thought or null/empty
            return createBelief("Error: Could not parse feedback score '" + feedbackInput + "'");
        }
    }
}
