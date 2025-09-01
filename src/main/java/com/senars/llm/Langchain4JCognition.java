package com.senars.llm;

import com.senars.core.Sessions;
import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.cycle.Cognition;
import com.senars.systems.Memory;
import com.senars.xai.Explain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * An implementation of the ICognitiveProcessor that uses Langchain4j to interact with a large language model.
 * This class orchestrates the process of context assembly, prompt generation, LLM interaction, and output parsing.
 */
public class Langchain4JCognition implements Cognition {

    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4JCognition.class);
    private static final int SIMILAR_THOUGHTS_COUNT = 5;

    private final ChatLanguageModel chatModel;
    private final Memory memoryNexus;
    private final PromptBuilder promptBuilder;
    private final StructuredOutputParser outputParser;
    private final Sessions sessions;
    private final Explain explain;

    /**
     * Constructs a new Langchain4jCognitiveProcessor.
     *
     * @param chat          The Langchain4j chat model to use for LLM interaction.
     * @param memory        The memory nexus for retrieving context and schemas.
     * @param promptBuilder The builder responsible for creating prompts.
     * @param outputParser  The parser for interpreting LLM responses.
     * @param sessions      The session manager, used for context like the last action.
     * @param explain       The explanation engine.
     */
    public Langchain4JCognition(
            ChatLanguageModel chat,
            Memory memory,
            PromptBuilder promptBuilder,
            StructuredOutputParser outputParser,
            Sessions sessions,
            Explain explain
    ) {
        this.chatModel = Objects.requireNonNull(chat, "chatModel cannot be null");
        this.memoryNexus = Objects.requireNonNull(memory, "memoryNexus cannot be null");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder cannot be null");
        this.outputParser = Objects.requireNonNull(outputParser, "outputParser cannot be null");
        this.sessions = Objects.requireNonNull(sessions, "sessions cannot be null");
        this.explain = Objects.requireNonNull(explain, "explain cannot be null");
    }

    @Override
    public List<Thought> process(Thought focusThought) {
        LOGGER.info("Processing thought: {} of type {}", focusThought.id(), focusThought.metadata().type());

        if (focusThought.metadata().type() == ThoughtType.EXPLANATION_REQUEST) {
            return handleExplanationRequest(focusThought);
        }

        // Step 1: Context Assembly
        LOGGER.debug("Assembling context for thought: {}", focusThought.id());

        // Get trace context (the direct history of this thought)
        List<Thought> traceContext = memoryNexus.getTrace(focusThought.id());
        LOGGER.debug("Retrieved {} thoughts from trace.", traceContext.size());

        // Get semantic context (similar thoughts)
        List<Thought> semanticContext = new ArrayList<>();
        if (focusThought.content().embedding() != null && !focusThought.content().embedding().isEmpty()) {
            semanticContext = memoryNexus.retrieveSimilar(focusThought.content().embedding(), SIMILAR_THOUGHTS_COUNT);
            LOGGER.debug("Retrieved {} similar thoughts from vector store.", semanticContext.size());
        } else {
            LOGGER.debug("Focus thought has no embedding, skipping semantic search.");
        }

        // Combine and deduplicate context
        Set<Thought> combinedContextSet = new HashSet<>(traceContext);
        combinedContextSet.addAll(semanticContext);
        // Remove the focus thought itself from the context, as it's the subject of the prompt
        combinedContextSet.remove(focusThought);

        List<Thought> context = new ArrayList<>(combinedContextSet);
        LOGGER.info("Assembled a total of {} unique context thoughts.", context.size());

        // For now, schema is empty as per the plan.
        Thought schema = null;

        // 2. Prompt Generation
        String prompt = promptBuilder.build(schema, focusThought, context);
        LOGGER.debug("Generated prompt: {}", prompt);

        // 3. LLM Call
        Response<AiMessage> response = chatModel.generate(UserMessage.from(prompt));
        String responseText = response.content().text();
        LOGGER.debug("Received response: {}", responseText);


        // 4. Output Parsing
        List<Thought> newThoughts = outputParser.parse(responseText);
        LOGGER.info("Generated {} new thoughts.", newThoughts.size());

        return newThoughts;
    }

    private List<Thought> handleExplanationRequest(Thought explanationRequest) {
        String targetId = explanationRequest.content().text();
        Optional<Thought> targetThoughtOpt;

        if ("last_action".equals(targetId)) {
            targetThoughtOpt = sessions.getLastActionPlan();
            if (targetThoughtOpt.isEmpty()) {
                return List.of(createSimpleReport("No last action has been recorded to explain.", explanationRequest.id()));
            }
        } else {
            targetThoughtOpt = memoryNexus.getThoughtById(targetId);
            if (targetThoughtOpt.isEmpty()) {
                return List.of(createSimpleReport("Could not find a thought with ID '" + targetId + "' to explain.", explanationRequest.id()));
            }
        }

        Thought targetThought = targetThoughtOpt.get();
        List<Thought> trace = explain.getTrace(targetThought);
        if (trace.isEmpty()) {
            return List.of(createSimpleReport("The thought '" + targetThought.content().text() + "' has no reasoning trace.", explanationRequest.id()));
        }

        String formattedTrace = explain.formatTrace(trace, targetThought);
        String prompt = promptBuilder.buildExplanationPrompt(formattedTrace);

        Response<AiMessage> response = chatModel.generate(UserMessage.from(prompt));
        String narrative = response.content().text();

        return List.of(createSimpleReport(narrative, explanationRequest.id()));
    }

    private Thought createSimpleReport(String text, String originatingRequestId) {
        ThoughtContent content = new ThoughtContent(text, null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.REPORT,
                ThoughtOrigin.LLM_INFERENCE,
                List.of(originatingRequestId),
                Instant.now()
        );
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0); // Reports are high clarity
        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
