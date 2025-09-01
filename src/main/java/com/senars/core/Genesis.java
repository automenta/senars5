package com.senars.core;

import com.senars.motive.Drive;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The Genesis class is responsible for creating the initial, immutable set of Thoughts
 * that every SeNARS instance starts with, such as the foundational Drives.
 */
public class Genesis {

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
                    null  // feedback
                ),
                new ThoughtState(
                    1.0, // clarity: Drives are foundational truths
                    0.0, // salience: To be calculated by the funnel
                    1.0  // activation: Drives are always active
                ),
                new ThoughtMetadata(
                    ThoughtType.BELIEF, // Drives are foundational beliefs about what is important
                    ThoughtOrigin.SYSTEM,
                    List.of(), // No trace for genesis thoughts
                    Instant.now()
                )
            );
            drives.add(driveThought);
        }
        return List.copyOf(drives);
    }

    private static String getDriveText(Drive drive) {
        return switch (drive) {
            case MAINTAIN_COHERENCE -> "The drive to find and resolve contradictions within the Memory Nexus.";
            case REDUCE_UNCERTAINTY -> "The drive to seek information that increases the Clarity of low-clarity Thoughts.";
            case ACQUIRE_KNOWLEDGE -> "The drive to explore novel information and synthesize new BELIEF or SCHEMA Thoughts.";
            case MAINTAIN_COGNITIVE_INTEGRITY ->
                "The meta-drive for self-improvement, making Thoughts about the system's own performance, health, and SCHEMA efficacy inherently salient.";
        };
    }
}
