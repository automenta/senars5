package com.senars.systems.perception;

import com.senars.core.*;
import com.senars.cycle.PerceptionChannel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A perception channel that monitors a directory for new files.
 * When a new file is detected, it reads its content and creates a BELIEF Thought.
 * It keeps track of processed files to avoid reprocessing them in subsequent cycles.
 */
public class FilePerceptionChannel implements PerceptionChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePerceptionChannel.class);
    private final Path directoryToMonitor;
    private final EmbeddingModel embeddingModel;
    private final Set<String> processedFiles = new HashSet<>();

    /**
     * Constructs a FilePerceptionChannel.
     *
     * @param directoryPath The path to the directory to monitor. If it doesn't exist, it will be created.
     * @param embeddingModel The model to use for generating embeddings for the file content.
     */
    public FilePerceptionChannel(String directoryPath, EmbeddingModel embeddingModel) {
        this.directoryToMonitor = Paths.get(directoryPath);
        this.embeddingModel = Objects.requireNonNull(embeddingModel);

        if (!Files.isDirectory(this.directoryToMonitor)) {
            try {
                Files.createDirectories(this.directoryToMonitor);
                LOGGER.info("Created perception directory: {}", this.directoryToMonitor);
            } catch (IOException e) {
                throw new IllegalArgumentException("Perception directory does not exist and could not be created: " + directoryPath, e);
            }
        }
    }

    @Override
    public List<Thought> perceive() {
        List<Thought> newThoughts = new ArrayList<>();
        File[] files = directoryToMonitor.toFile().listFiles();

        if (files == null) {
            LOGGER.warn("Could not list files in perception directory: {}", directoryToMonitor);
            return newThoughts;
        }

        for (File file : files) {
            if (file.isFile() && !processedFiles.contains(file.getName())) {
                try {
                    String content = Files.readString(file.toPath());
                    if (!content.isBlank()) {
                        Thought belief = createBeliefFromFile(content, file.getName());
                        newThoughts.add(belief);
                        LOGGER.info("Perceived new file: {}. Created BELIEF thought.", file.getName());
                    }
                    // Mark file as processed even if empty to avoid re-checking
                    processedFiles.add(file.getName());
                } catch (IOException e) {
                    LOGGER.error("Error reading file: {}", file.getName(), e);
                    // Add to processed files to avoid retrying a file that can't be read
                    processedFiles.add(file.getName());
                }
            }
        }
        return newThoughts;
    }

    private Thought createBeliefFromFile(String text, String sourceFile) {
        String thoughtText = "Information from file '" + sourceFile + "':\n" + text;
        List<Double> embedding = generateEmbedding(thoughtText);

        ThoughtContent content = new ThoughtContent(thoughtText, null, embedding, null, null, null, null);
        ThoughtMeta metadata = new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.PERCEPTION, Collections.emptyList(), java.time.Instant.now());
        // Initial state for perceived thoughts from a trusted source (file system)
        ThoughtState state = new ThoughtState(0.8, 0.5, 1.0); // High clarity, medium salience, high activation

        return new Thought(UUID.randomUUID().toString(), content, state, metadata);
    }

    private List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            return embedding.vectorAsList().stream().map(Float::doubleValue).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Failed to generate embedding for text.", e);
            return Collections.emptyList();
        }
    }
}
