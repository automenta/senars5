package com.senars.tools;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.systems.Memory;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmbeddingGenerationTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingGenerationTool.class);

    private final Memory memory;
    private final EmbeddingModel embeddingModel;

    public EmbeddingGenerationTool(Memory memory, EmbeddingModel embeddingModel) {
        this.memory = memory;
        this.embeddingModel = embeddingModel;
    }

    @Tool("Generates and saves a vector embedding for a thought that is missing one.")
    public String generate_embedding(String thoughtId) {
        LOGGER.info("Tool 'generate_embedding' called for thoughtId: {}", thoughtId);
        Optional<Thought> thoughtOpt = memory.getThoughtById(thoughtId);

        if (thoughtOpt.isEmpty()) {
            String errorMsg = "Thought with ID " + thoughtId + " not found.";
            LOGGER.error(errorMsg);
            return "Error: " + errorMsg;
        }

        Thought thought = thoughtOpt.get();
        if (thought.content().text() == null || thought.content().text().isEmpty()) {
            String errorMsg = "Thought with ID " + thoughtId + " has no text content to embed.";
            LOGGER.warn(errorMsg);
            return "Warning: " + errorMsg;
        }

        if (thought.content().embedding() != null && !thought.content().embedding().isEmpty()) {
            String msg = "Thought with ID " + thoughtId + " already has an embedding.";
            LOGGER.info(msg);
            return msg;
        }

        try {
            LOGGER.debug("Generating embedding for text: '{}'", thought.content().text());
            List<Double> embedding = new ArrayList<>();
            for (float f : embeddingModel.embed(thought.content().text()).content().vector()) {
                embedding.add((double) f);
            }

            ThoughtContent newContent = new ThoughtContent(
                    thought.content().text(),
                    thought.content().symbolic(),
                    embedding,
                    thought.content().perceptual(),
                    thought.content().procedural(),
                    thought.content().feedback(),
                    thought.content().rules()
            );

            Thought updatedThought = new Thought(
                    thought.id(),
                    newContent,
                    thought.state(),
                    thought.metadata()
            );

            memory.saveThought(updatedThought);
            String successMsg = "Successfully generated and saved embedding for thought " + thoughtId;
            LOGGER.info(successMsg);
            return successMsg;

        } catch (Exception e) {
            String errorMsg = "Failed to generate or save embedding for thought " + thoughtId;
            LOGGER.error(errorMsg, e);
            return "Error: " + errorMsg + ". " + e.getMessage();
        }
    }
}
