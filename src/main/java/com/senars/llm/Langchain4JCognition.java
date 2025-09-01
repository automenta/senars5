package com.senars.llm;

import com.senars.core.*;
import com.senars.cycle.Cognition;
import com.senars.systems.Memory;
import com.senars.cycle.Inference;
import com.senars.xai.Explain;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import com.google.gson.Gson;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of the ICognitiveProcessor that uses Langchain4j to interact with a large language model.
 * This class orchestrates the process of context assembly, prompt generation, LLM interaction, and output parsing.
 */
public class Langchain4JCognition implements Cognition {

    private static final Logger LOGGER = LoggerFactory.getLogger(Langchain4JCognition.class);
    private static final int SIMILAR_THOUGHTS_COUNT = 5;
    private static final int SCHEMA_COUNT = 1;
    private static final String LOGICAL_QUERY_SYMBOL = "senars:logical_query";


    private final ChatLanguageModel chatModel;
    private final Memory memory;
    private final PromptBuilder promptBuilder;
    private final StructuredOutputParser outputParser;
    private final Sessions sessions;
    private final Explain explain;
    private final Inference inference;
    private final ToolKit toolKit;
    private final Gson gson = new Gson();

    /**
     * Constructs a new Langchain4jCognitiveProcessor.
     *
     * @param chat          The Langchain4j chat model to use for LLM interaction.
     * @param memory        The memory nexus for retrieving context and schemas.
     * @param promptBuilder The builder responsible for creating prompts.
     * @param outputParser  The parser for interpreting LLM responses.
     * @param sessions      The session manager, used for context like the last action.
     * @param explain       The explanation engine.
     * @param inference     The logical inference engine.
     * @param toolKit       The toolkit containing available tools.
     */
    public Langchain4JCognition(
            ChatLanguageModel chat,
            Memory memory,
            PromptBuilder promptBuilder,
            StructuredOutputParser outputParser,
            Sessions sessions,
            Explain explain,
            Inference inference,
            ToolKit toolKit
    ) {
        this.chatModel = requireNonNull(chat, "chatModel cannot be null");
        this.memory = requireNonNull(memory, "memory cannot be null");
        this.promptBuilder = requireNonNull(promptBuilder, "promptBuilder cannot be null");
        this.outputParser = requireNonNull(outputParser, "outputParser cannot be null");
        this.sessions = requireNonNull(sessions, "sessions cannot be null");
        this.explain = requireNonNull(explain, "explain cannot be null");
        this.inference = requireNonNull(inference, "inference cannot be null");
        this.toolKit = requireNonNull(toolKit, "toolKit cannot be null");
    }

    @Override
    public List<Thought> process(Thought focusThought) {
        LOGGER.info("Processing thought: {} of type {}", focusThought.id(), focusThought.metadata().type());

        if (focusThought.metadata().type() == ThoughtType.GOAL && LOGICAL_QUERY_SYMBOL.equals(focusThought.content().symbolic())) {
            LOGGER.info("Detected logical query. Delegating to Inference engine.");
            return inference.reason(focusThought);
        }

        if (focusThought.metadata().type() == ThoughtType.EXPLANATION_REQUEST) {
            return handleExplanationRequest(focusThought);
        }

        // Step 1: Context Assembly
        List<Thought> context = assembleContext(focusThought);

        // Step 2: Schema Selection
        Thought schema = findRelevantSchema(focusThought);
        if (schema != null) {
            LOGGER.info("Found relevant schema: {}", schema.id());
        } else {
            LOGGER.info("No relevant schema found. Using fallback prompt.");
        }

        // Step 3: Prompt Generation
        List<ToolSpecification> toolSpecifications = toolKit.getToolSpecifications();
        String prompt = promptBuilder.build(schema, focusThought, context, toolSpecifications);
        LOGGER.debug("Generated prompt: {}", prompt);

        // Step 4: LLM Call
        Response<AiMessage> response = chatModel.generate(UserMessage.from(prompt));
        String responseText = response.content().text();
        LOGGER.debug("Received response: {}", responseText);

        // Step 5: Output Parsing and Thought Generation
        return parseResponse(responseText, focusThought);
    }

    private List<Thought> parseResponse(String responseText, Thought focusThought) {
        // Attempt to parse as a tool execution request first
        ToolExecutionRequest toolRequest = toolKit.parse(responseText);
        if (toolRequest != null) {
            LOGGER.info("LLM requested to execute tool: {}", toolRequest.name());
            ThoughtContent content = new ThoughtContent(
                    "Authorize execution of " + toolRequest.name(),
                    gson.toJson(toolRequest), // Store the full request as symbolic content
                    null, null, null, null, null
            );
            ThoughtMeta meta = new ThoughtMeta(
                    ThoughtType.ACTION_PLAN,
                    ThoughtOrigin.LLM_INFERENCE,
                    List.of(focusThought.id()), // Trace back to the thought that triggered this plan
                    Instant.now()
            );
            ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
            Thought actionPlan = new Thought(UUID.randomUUID().toString(), content, state, meta);
            return List.of(actionPlan);
        }

        // If not a tool request, parse as a standard structured response
        LOGGER.info("Response is not a tool request. Parsing as structured output.");
        return outputParser.parse(responseText);
    }

    private List<Thought> assembleContext(Thought focusThought) {
        LOGGER.debug("Assembling context for thought: {}", focusThought.id());
        List<Thought> traceContext = memory.getTrace(focusThought.id());
        LOGGER.debug("Retrieved {} thoughts from trace.", traceContext.size());

        List<Thought> semanticContext = new ArrayList<>();
        if (focusThought.content().embedding() != null && !focusThought.content().embedding().isEmpty()) {
            semanticContext = memory.retrieveSimilar(focusThought.content().embedding(), SIMILAR_THOUGHTS_COUNT);
            LOGGER.debug("Retrieved {} similar thoughts from vector store.", semanticContext.size());
        } else {
            LOGGER.debug("Focus thought has no embedding, skipping semantic search.");
        }

        Set<Thought> combinedContextSet = new HashSet<>(traceContext);
        combinedContextSet.addAll(semanticContext);
        combinedContextSet.remove(focusThought);

        List<Thought> context = new ArrayList<>(combinedContextSet);
        LOGGER.info("Assembled a total of {} unique context thoughts.", context.size());
        return context;
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
            targetThoughtOpt = memory.getThoughtById(targetId);
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
        ThoughtContent content = new ThoughtContent(text, null, null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.REPORT,
                ThoughtOrigin.LLM_INFERENCE,
                List.of(originatingRequestId),
                Instant.now()
        );
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0); // Reports are high clarity
        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }

    /**
     * Finds the most relevant schema for a given thought.
     *
     * @param focusThought The thought to find a schema for.
     * @return An Optional containing the most relevant schema, or empty if none is found.
     */
    private Thought findRelevantSchema(Thought focusThought) {
        if (focusThought.content().embedding() == null || focusThought.content().embedding().isEmpty()) {
            LOGGER.debug("Focus thought has no embedding, cannot search for schema.");
            return null;
        }

        List<Thought> schemas = memory.retrieveSimilar(
                focusThought.content().embedding(),
                SCHEMA_COUNT,
                ThoughtType.SCHEMA
        );

        if (schemas.isEmpty()) {
            return null;
        }
        return schemas.getFirst();
    }
}
