package com.senars.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.senars.motive.Drive;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Genesis class is responsible for creating the initial, immutable set of Thoughts
 * that every SeNARS instance starts with, such as foundational Drives and knowledge.
 */
public class Genesis {

    public static final String LOGICAL_ACTION_SCHEMA_SYMBOL = "senars:schema:logical-action-v1";
    public static final String FAILURE_RECOVERY_SCHEMA_SYMBOL = "senars:schema:failure-recovery-v1";
    private static final Logger LOGGER = LoggerFactory.getLogger(Genesis.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Loads initial belief Thoughts from a JSON resource file.
     * After loading, it generates and sets the embedding for each thought.
     *
     * @param resourcePath   The path to the JSON file in the resources folder.
     * @param embeddingModel The model to use for generating embeddings.
     * @return A list of Thought objects.
     */
    public static List<Thought> loadKnowledgeFromFile(String resourcePath, EmbeddingModel embeddingModel) {
        try (InputStream inputStream = Genesis.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                LOGGER.error("Genesis resource file not found: {}", resourcePath);
                return Collections.emptyList();
            }

            List<Thought> thoughts = OBJECT_MAPPER.readValue(inputStream, new TypeReference<>() {
            });

            // Generate and set embeddings for each thought
            return thoughts.stream()
                    .map(thought -> {
                        var content = thought.content();
                        var text = content.text();
                        if (text != null && !text.isEmpty()) {
                            List<Double> embedding = new ArrayList<>();
                            for (float f : embeddingModel.embed(text).content().vector()) {
                                embedding.add((double) f);
                            }
                            // Create a new Thought with the updated embedding
                            return new Thought(
                                    thought.id(),
                                    new ThoughtContent(
                                            text,
                                            content.symbolic(),
                                            embedding,
                                            content.perceptual(),
                                            content.procedural(),
                                            content.feedback(),
                                            content.rules()
                                    ),
                                    thought.state(),
                                    thought.metadata()
                            );
                        }
                        return thought;
                    })
                    .toList();

        } catch (Exception e) {
            LOGGER.error("Failed to load or process genesis knowledge from {}", resourcePath, e);
            return Collections.emptyList();
        }
    }

    /**
     * Loads initial schema Thoughts from a JSON resource file.
     * After loading, it generates and sets the embedding for each thought.
     *
     * @param resourcePath   The path to the JSON file in the resources folder.
     * @param embeddingModel The model to use for generating embeddings.
     * @return A list of Thought objects.
     */
    public static List<Thought> loadSchemasFromFile(String resourcePath, EmbeddingModel embeddingModel) {
        return loadKnowledgeFromFile(resourcePath, embeddingModel); // Re-use the same logic
    }


    /**
     * Creates the list of foundational Drive thoughts for the system.
     * Each drive is represented as a high-clarity BELIEF thought.
     *
     * @param embeddingModel The embedding model to use for generating vector embeddings for the drives' text.
     * @return A list of Thought objects representing the genesis drives.
     */
    public static List<Thought> createGenesisDrives(EmbeddingModel embeddingModel) {
        List<Thought> drives = new ArrayList<>();

        for (Drive driveEnum : Drive.values()) {
            String text = getDriveText(driveEnum);
            List<Double> embedding = new ArrayList<>();
            for (float f : embeddingModel.embed(text).content().vector()) {
                embedding.add((double) f);
            }


            Thought driveThought = new Thought(
                    "drive-" + driveEnum.name().toLowerCase(),
                    new ThoughtContent(
                            text,
                            null, // symbolic
                            embedding,
                            null, // perceptual
                            null, // procedural
                            null,  // feedback
                            null // rules
                    ),
                    new ThoughtState(
                            1.0, // clarity: Drives are foundational truths
                            0.0, // salience: To be calculated by the funnel
                            1.0  // activation: Drives are always active
                    ),
                    new ThoughtMeta(
                            ThoughtType.DRIVE, // Drives are a distinct type of thought
                            ThoughtOrigin.SYSTEM,
                            List.of(), // No trace for genesis thoughts
                            Instant.now()
                    )
            );
            drives.add(driveThought);
        }
        return List.copyOf(drives);
    }

    /**
     * Creates the system's prime ambition as a GOAL thought.
     *
     * @param embeddingModel The model to generate the embedding for the ambition's text.
     * @return A Thought object representing the prime ambition.
     */
    public static Thought createPrimeAmbition(EmbeddingModel embeddingModel) {
        String text = "My primary ambition is to understand my own architecture, purpose, and capabilities based on my foundational knowledge.";
        List<Double> embedding = new ArrayList<>();
        for (float f : embeddingModel.embed(text).content().vector()) {
            embedding.add((double) f);
        }

        return new Thought(
                "ambition-genesis-1",
                new ThoughtContent(
                        text,
                        null,
                        embedding,
                        null,
                        null,
                        null,
                        null
                ),
                new ThoughtState(
                        1.0, // clarity
                        0.0, // salience
                        1.0  // activation
                ),
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.SYSTEM,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * Creates the schema for formulating and executing logical queries.
     *
     * @param embeddingModel The model to generate the embedding.
     * @return A Thought object representing the logical action schema.
     */
    public static Thought createLogicalActionSchema(EmbeddingModel embeddingModel) {
        String text = "The user's goal can be answered using formal logic. Formulate a precise Prolog query to answer the goal and create a plan to execute it with the `logical_inference.executeQuery` tool.";
        List<Double> embedding = new ArrayList<>();
        for (float f : embeddingModel.embed(text).content().vector()) {
            embedding.add((double) f);
        }

        return new Thought(
                "schema-logical-action-1",
                new ThoughtContent(
                        text,
                        LOGICAL_ACTION_SCHEMA_SYMBOL,
                        embedding,
                        null, null, null, null
                ),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(
                        ThoughtType.SCHEMA,
                        ThoughtOrigin.SYSTEM,
                        List.of(),
                        Instant.now()
                )
        );
    }

    /**
     * Creates the schema for recovering from a failed tool execution.
     *
     * @param embeddingModel The model to generate the embedding.
     * @return A Thought object representing the failure recovery schema.
     */
    public static Thought createFailureRecoverySchema(EmbeddingModel embeddingModel) {
        String text = "A tool execution has failed. The current focus describes the tool and the error. Analyze this failure and formulate a new plan to achieve the original objective. Consider using alternative tools or methods. If the failure was due to missing information, create a plan to find that information.";
        List<Double> embedding = new ArrayList<>();
        for (float f : embeddingModel.embed(text).content().vector()) {
            embedding.add((double) f);
        }

        return new Thought(
                "schema-failure-recovery-1",
                new ThoughtContent(
                        text,
                        FAILURE_RECOVERY_SCHEMA_SYMBOL,
                        embedding,
                        null, null, null, null
                ),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(
                        ThoughtType.SCHEMA,
                        ThoughtOrigin.SYSTEM,
                        List.of(),
                        Instant.now()
                )
        );
    }

    private static String getDriveText(Drive drive) {
        return switch (drive) {
            case MAINTAIN_COHERENCE -> "The drive to find and resolve contradictions within the Memory Nexus.";
            case REDUCE_UNCERTAINTY ->
                    "The drive to seek information that increases the Clarity of low-clarity Thoughts.";
            case ACQUIRE_KNOWLEDGE ->
                    "The drive to explore novel information and synthesize new BELIEF or SCHEMA Thoughts.";
            case MAINTAIN_COGNITIVE_INTEGRITY ->
                    "The meta-drive for self-improvement, making Thoughts about the system's own performance, health, and SCHEMA efficacy inherently salient.";
            case ENRICH_KNOWLEDGE ->
                    "The drive to complete and enrich existing knowledge, for example by generating missing vector embeddings for thoughts that have text content.";
        };
    }

    /**
     * Creates the initial research goal for demonstration purposes.
     *
     * @param embeddingModel The model to generate the embedding for the goal's text.
     * @return A Thought object representing the research goal.
     */
    public static Thought createResearchGoal(EmbeddingModel embeddingModel) {
        String text = "Provide a summary of the latest announcements on the official OpenAI blog.";
        List<Double> embedding = new ArrayList<>();
        for (float f : embeddingModel.embed(text).content().vector()) {
            embedding.add((double) f);
        }

        return new Thought(
                "goal-genesis-research-1",
                new ThoughtContent(
                        text,
                        null,
                        embedding,
                        null,
                        null,
                        null,
                        null
                ),
                new ThoughtState(
                        1.0, // clarity
                        100.0, // high initial salience to kick things off
                        1.0  // activation
                ),
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.SYSTEM,
                        List.of(),
                        Instant.now()
                )
        );
    }
}
