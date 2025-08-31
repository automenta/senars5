package com.senars.llm;

import com.senars.core.Thought;
import com.senars.cycle.ICognitiveProcessor;
import com.senars.systems.IMemoryNexus;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of the ICognitiveProcessor that uses Langchain4j to interact with a large language model.
 * This class orchestrates the process of context assembly, prompt generation, LLM interaction, and output parsing.
 */
public class Langchain4jCognitiveProcessor implements ICognitiveProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4jCognitiveProcessor.class);

    private final ChatModel chatModel;
    private final IMemoryNexus memoryNexus;
    private final PromptBuilder promptBuilder;
    private final StructuredOutputParser outputParser;

    /**
     * Constructs a new Langchain4jCognitiveProcessor.
     *
     * @param chatModel     The Langchain4j chat model to use for LLM interaction.
     * @param memoryNexus   The memory nexus for retrieving context and schemas.
     * @param promptBuilder The builder responsible for creating prompts.
     * @param outputParser  The parser for interpreting LLM responses.
     */
    public Langchain4jCognitiveProcessor(
            ChatModel chatModel,
            IMemoryNexus memoryNexus,
            PromptBuilder promptBuilder,
            StructuredOutputParser outputParser
    ) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel cannot be null");
        this.memoryNexus = Objects.requireNonNull(memoryNexus, "memoryNexus cannot be null");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder cannot be null");
        this.outputParser = Objects.requireNonNull(outputParser, "outputParser cannot be null");
    }

    @Override
    public List<Thought> process(Thought focusThought) {
        LOGGER.info("Processing thought: {}", focusThought.id());

        // For now, context and schema are empty as per the plan.
        // This will be expanded in later steps.
        List<Thought> context = Collections.emptyList();
        Thought schema = null;

        // 1. Prompt Generation
        String prompt = promptBuilder.build(schema, focusThought, context);
        LOGGER.debug("Generated prompt: {}", prompt);

        // 2. LLM Call
        String response = chatModel.chat(prompt);
        LOGGER.debug("Received response: {}", response);

        // 3. Output Parsing
        List<Thought> newThoughts = outputParser.parse(response);
        LOGGER.info("Generated {} new thoughts.", newThoughts.size());

        return newThoughts;
    }
}
