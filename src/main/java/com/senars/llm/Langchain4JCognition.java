package com.senars.llm;

import com.senars.core.Thought;
import com.senars.cycle.Cognition;
import com.senars.systems.Memory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An implementation of the ICognitiveProcessor that uses Langchain4j to interact with a large language model.
 * This class orchestrates the process of context assembly, prompt generation, LLM interaction, and output parsing.
 */
public class Langchain4JCognition implements Cognition {

    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4JCognition.class);

    private final ChatLanguageModel chatModel;
    private final Memory memoryNexus;
    private final PromptBuilder promptBuilder;
    private final StructuredOutputParser outputParser;

    /**
     * Constructs a new Langchain4jCognitiveProcessor.
     *
     * @param chat     The Langchain4j chat model to use for LLM interaction.
     * @param memory   The memory nexus for retrieving context and schemas.
     * @param promptBuilder The builder responsible for creating prompts.
     * @param outputParser  The parser for interpreting LLM responses.
     */
    public Langchain4JCognition(
            ChatLanguageModel chat,
            Memory memory,
            PromptBuilder promptBuilder,
            StructuredOutputParser outputParser
    ) {
        this.chatModel = Objects.requireNonNull(chat, "chatModel cannot be null");
        this.memoryNexus = Objects.requireNonNull(memory, "memoryNexus cannot be null");
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
        Response<AiMessage> response = chatModel.generate(UserMessage.from(prompt));
        String responseText = response.content().text();
        LOGGER.debug("Received response: {}", responseText);


        // 3. Output Parsing
        List<Thought> newThoughts = outputParser.parse(responseText);
        LOGGER.info("Generated {} new thoughts.", newThoughts.size());

        return newThoughts;
    }
}
