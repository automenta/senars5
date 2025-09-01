package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.cycle.IPerceptionSystem;
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
public class ConsolePerceptionSystem implements IPerceptionSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsolePerceptionSystem.class);
    private final Scanner scanner;
    private final EmbeddingModel embeddingModel;

    public ConsolePerceptionSystem(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public List<Thought> perceive() {
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            try {
                if (input.toLowerCase().startsWith("goal:")) {
                    return Collections.singletonList(createGoal(input.substring(5).trim()));
                } else if (input.toLowerCase().startsWith("belief:")) {
                    return Collections.singletonList(createBelief(input.substring(7).trim()));
                } else if (input.toLowerCase().startsWith("question:")) {
                    return Collections.singletonList(createQuestion(input.substring(9).trim()));
                } else if (input.toLowerCase().startsWith("feedback:")) {
                    return Collections.singletonList(createFeedbackReport(input.substring(9).trim()));
                } else if (!input.isEmpty()) {
                    return Collections.singletonList(createBelief(input));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to create thought from input: '{}'", input, e);
                // Optionally, create a "parse_error" thought or just ignore.
            }
        }
        return Collections.emptyList();
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
        ThoughtMetadata metadata = new ThoughtMetadata(type, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
        ThoughtState state = new ThoughtState(0.9, 1.0, 1.0); // High clarity/salience for user input
        return new Thought(UUID.randomUUID().toString(), content, state, metadata);
    }

    private Thought createFeedbackReport(String feedbackInput) {
        try {
            double score = Double.parseDouble(feedbackInput);
            score = Math.max(0.0, Math.min(1.0, score)); // Clamp score to [0, 1]

            Feedback feedback = new Feedback("User console feedback.", score);
            ThoughtContent content = new ThoughtContent(
                    "User feedback report. Score: " + score,
                    null, null, null, null, feedback);

            // Note: The trace for this feedback will need to be added by the component that manages the session,
            // as the perception system itself doesn't know which action this feedback is for.
            ThoughtMetadata metadata = new ThoughtMetadata(ThoughtType.REPORT, ThoughtOrigin.USER, Collections.emptyList(), java.time.Instant.now());
            ThoughtState state = new ThoughtState(1.0, 1.0, 1.0); // Feedback is always high clarity
            return new Thought(UUID.randomUUID().toString(), content, state, metadata);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid feedback score format: '{}'. Must be a number.", feedbackInput);
            // Return a special error thought or null/empty
            return createBelief("Error: Could not parse feedback score '" + feedbackInput + "'");
        }
    }
}
