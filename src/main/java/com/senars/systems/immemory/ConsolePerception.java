package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.cycle.PerceptionChannel;
import com.senars.cycle.ShutdownException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of the Perception system that reads user input from the console.
 * It parses the input to create different types of Thoughts and generates embeddings for them.
 */
public class ConsolePerception implements PerceptionChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsolePerception.class);
    private final Scanner scanner;
    private final EmbeddingModel embeddingModel;

    public ConsolePerception(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.scanner = new Scanner(System.in);
        System.out.print("> "); // Initial prompt
    }

    @Override
    public List<Thought> perceive() throws ShutdownException {
        List<Thought> newThoughts = new ArrayList<>();

        // Poll for console input (non-blocking)
        try {
            if (System.in.available() > 0 && scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                System.out.print("> "); // Print prompt after input to avoid clutter

                if (handleSpecialCommands(input)) {
                    return newThoughts; // Command was handled, no thought produced
                }

                if (input.toLowerCase().startsWith("goal:")) {
                    newThoughts.add(createGoal(input.substring(5).trim()));
                } else if (input.toLowerCase().startsWith("belief:")) {
                    newThoughts.add(createBelief(input.substring(7).trim()));
                } else if (input.toLowerCase().startsWith("question:")) {
                    newThoughts.add(createQuestion(input.substring(9).trim()));
                } else if (input.toLowerCase().startsWith("why")) {
                    newThoughts.add(createExplanationRequest(input));
                } else if (!input.isEmpty()) {
                    // Default to creating a belief if no prefix is provided
                    newThoughts.add(createBelief(input));
                }
            }
        } catch (IOException | IllegalStateException e) {
            LOGGER.error("Error checking console input.", e);
        } catch (Exception e) {
            LOGGER.error("Failed to create thought from console input.", e);
        }

        return newThoughts;
    }

    private boolean handleSpecialCommands(String input) throws ShutdownException {
        String command = input.toLowerCase();
        if (command.equals("shutdown") || command.equals("exit")) {
            throw new ShutdownException("Shutdown command received from console.");
        }
        if (command.equals("help")) {
            printHelp();
            return true;
        }
        return false;
    }

    private void printHelp() {
        System.out.println("\n--- SeNARS Console Help ---");
        System.out.println("Create thoughts by typing a prefix followed by your text:");
        System.out.println("  goal: <your goal>       - Create a new goal for the system.");
        System.out.println("  belief: <a fact>        - Add a new belief to the system's memory.");
        System.out.println("  question: <your query>  - Ask a question.");
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

        ThoughtContent content = new ThoughtContent(targetId, null, null, null, null, null, null);
        ThoughtMeta metadata = new ThoughtMeta(ThoughtType.EXPLAIN, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
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
        ThoughtContent content = new ThoughtContent(text, null, embedding, null, null, null, null);
        ThoughtMeta metadata = new ThoughtMeta(type, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
        ThoughtState state = new ThoughtState(0.9, 1.0, 1.0); // High clarity/salience for user input
        return new Thought(UUID.randomUUID().toString(), content, state, metadata);
    }
}
