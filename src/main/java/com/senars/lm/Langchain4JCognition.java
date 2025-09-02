package com.senars.lm;

import com.google.gson.Gson;
import com.senars.core.Genesis;
import com.senars.core.Thought;
import com.senars.effort.LinearTextEffortModel;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtType;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtState;
import com.senars.cycle.Cognition;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.systems.Memory;
import com.senars.systems.ScoredThought;
import com.senars.xai.Explain;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
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
    private static final int SCHEMA_COUNT = 5; // Fetch more schemas to choose from
    private static final String GENERATE_EMBEDDING_SCHEMA_SYMBOL = "senars:generate_embedding_schema";
    private static final String REPLAN_SYMBOL = "senars:replan";


    private final ChatLanguageModel chatModel;
    private final Memory memory;
    private final PromptBuilder promptBuilder;
    private final StructuredOutputParser outputParser;
    private final Explain explain;
    private final ToolKit toolKit;
    private final EventBus eventBus;
    private final Gson gson = new Gson();
    private Thought lastActionPlan = null;

    /**
     * Constructs a new Langchain4jCognitiveProcessor.
     *
     * @param chat          The Langchain4j chat model to use for LLM interaction.
     * @param memory        The memory nexus for retrieving context and schemas.
     * @param promptBuilder The builder responsible for creating prompts.
     * @param outputParser  The parser for interpreting LLM responses.
     * @param explain       The explanation engine.
     * @param toolKit       The toolkit containing available tools.
     * @param eventBus      The event bus for publishing events.
     */
    public Langchain4JCognition(
            ChatLanguageModel chat,
            Memory memory,
            PromptBuilder promptBuilder,
            StructuredOutputParser outputParser,
            Explain explain,
            ToolKit toolKit,
            EventBus eventBus
    ) {
        this.chatModel = requireNonNull(chat, "chatModel cannot be null");
        this.memory = requireNonNull(memory, "memory cannot be null");
        this.promptBuilder = requireNonNull(promptBuilder, "promptBuilder cannot be null");
        this.outputParser = requireNonNull(outputParser, "outputParser cannot be null");
        this.explain = requireNonNull(explain, "explain cannot be null");
        this.toolKit = requireNonNull(toolKit, "toolKit cannot be null");
        this.eventBus = requireNonNull(eventBus, "eventBus cannot be null");
    }

    @Override
    public List<Thought> process(Thought focusThought) {
        eventBus.publish(new Events.CognitionStartEvent(focusThought));
        try {
            LOGGER.info("Processing thought: {} of type {}", focusThought.id(), focusThought.metadata().type());

            if (focusThought.metadata().type() == ThoughtType.GOAL && SchemaOptimizer.REWRITE_SCHEMA_SYMBOLIC.equals(focusThought.content().symbolic())) {
                LOGGER.info("Detected schema rewrite goal. Delegating to self-optimization handler.");
                return handleSchemaRewriteGoal(focusThought);
            }

            if (focusThought.metadata().type() == ThoughtType.GOAL && EffortModelOptimizer.REWRITE_EFFORT_MODEL_SYMBOLIC.equals(focusThought.content().symbolic())) {
                LOGGER.info("Detected effort model rewrite goal. Delegating to self-optimization handler.");
                return handleEffortModelRewriteGoal(focusThought);
            }

            if (focusThought.metadata().type() == ThoughtType.EXPLAIN) {
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

            // Handle special, non-LLM schemas
            if (schema != null && GENERATE_EMBEDDING_SCHEMA_SYMBOL.equals(schema.content().symbolic())) {
                LOGGER.info("Bypassing LLM for internal embedding generation schema.");
                return createGenerateEmbeddingActionPlan(focusThought, schema);
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
            return parseResponse(responseText, focusThought, schema);
        } catch (Exception e) {
            LOGGER.error("Error during cognitive processing for thought: {}", focusThought.id(), e);
            eventBus.publish(new Events.CognitionErrorEvent(focusThought, e));
            return Collections.emptyList(); // Return empty list to signify failure
        } finally {
            eventBus.publish(new Events.CognitionEndEvent(focusThought));
        }
    }

    private List<Thought> createGenerateEmbeddingActionPlan(Thought focusThought, Thought schema) {
        // We construct the tool call JSON manually for this internal action.
        String toolCallJson = String.format(
                "{\"name\":\"generate_embedding\",\"arguments\":{\"thoughtId\":\"%s\"}}",
                focusThought.id()
        );

        ToolExecutionRequest toolRequest = toolKit.parse(toolCallJson);
        return createActionPlan(toolRequest, focusThought, schema);
    }

    List<Thought> parseResponse(String responseText, Thought focusThought, Thought schema) {
        // First, always check if the response is a tool execution request.
        ToolExecutionRequest toolRequest = toolKit.parse(responseText);
        if (toolRequest != null) {
            LOGGER.info("LLM requested to execute tool: {}", toolRequest.name());
            return createActionPlan(toolRequest, focusThought, schema);
        }

        // If not a tool call, assume it's a structured Thought or array of Thoughts.
        LOGGER.debug("Response is not a tool call. Attempting to parse as structured output.");
        List<Thought> newThoughts = outputParser.parse(responseText);

        // The parser returns a single REPORT thought on failure.
        // If we get a report, but the schema wasn't for reporting, something went wrong.
        boolean parseFailed = newThoughts.size() == 1 &&
                              newThoughts.getFirst().metadata().type() == ThoughtType.REPORT &&
                              (schema == null || !"senars:schema:direct_reasoning".equals(schema.content().symbolic()));

        if (parseFailed) {
            LOGGER.warn("Parsing response failed or produced an unexpected report. Creating a replan goal.");
            return createReplanningGoal(focusThought, responseText);
        }

        LOGGER.info("Successfully parsed {} new thought(s) from LLM response.", newThoughts.size());
        return newThoughts;
    }

    private List<Thought> createReplanningGoal(Thought failedThought, String failedResponse) {
        String goalText = String.format(
                "The previous attempt to process thought %s failed. The LLM response could not be parsed. A new approach is needed. Faulty response: %s",
                failedThought.id(),
                failedResponse
        );

        ThoughtContent content = new ThoughtContent(
                goalText,
                REPLAN_SYMBOL, // Symbolic name for replanning
                null, null, null, null, null
        );

        // The new goal should trace back to the original thought that started the failed chain.
        List<String> newTrace = new ArrayList<>(failedThought.metadata().trace());
        newTrace.add(failedThought.id());


        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                newTrace,
                Instant.now()
        );

        // Very high salience to ensure it's the next thing we deal with
        ThoughtState state = new ThoughtState(1.0, 150.0, 1.0);
        Thought replanGoal = new Thought(UUID.randomUUID().toString(), content, state, meta);
        return List.of(replanGoal);
    }

    private List<Thought> createActionPlan(ToolExecutionRequest toolRequest, Thought focusThought, Thought schema) {
        ThoughtContent content = new ThoughtContent(
                "Authorize execution of " + toolRequest.name(),
                gson.toJson(toolRequest), // Store the full request as symbolic content
                null, null, null, null, null
        );
        List<String> trace = new ArrayList<>();
        trace.add(focusThought.id());
        if (schema != null) {
            trace.add(schema.id());
        }

        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.ACTION,
                ThoughtOrigin.LLM_INFERENCE,
                trace, // Trace back to the focus thought AND the schema used
                Instant.now()
        );
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
        Thought actionPlan = new Thought(UUID.randomUUID().toString(), content, state, meta);
        this.lastActionPlan = actionPlan;
        return List.of(actionPlan);
    }

    private List<Thought> assembleContext(Thought focusThought) {
        LOGGER.debug("Assembling context for thought: {}", focusThought.id());
        List<Thought> traceContext = memory.getTrace(focusThought.id());
        LOGGER.debug("Retrieved {} thoughts from trace.", traceContext.size());

        List<Thought> semanticContext = new ArrayList<>();
        if (focusThought.content().embedding() != null && !focusThought.content().embedding().isEmpty()) {
            List<ScoredThought> scoredContext = memory.retrieveSimilar(focusThought.content().embedding(), SIMILAR_THOUGHTS_COUNT);
            semanticContext = scoredContext.stream().map(ScoredThought::thought).toList();
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
            targetThoughtOpt = Optional.ofNullable(this.lastActionPlan);
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
     * Finds the most relevant schema for a given thought by combining semantic similarity and schema clarity.
     *
     * @param focusThought The thought to find a schema for.
     * @return An Optional containing the most relevant schema, or empty if none is found.
     */
    public Thought findRelevantSchema(Thought focusThought) {
        // Priority 1: Check for specific, system-driven goals that require a fixed schema.
        if (focusThought.metadata().type() == ThoughtType.GOAL && focusThought.metadata().origin() == ThoughtOrigin.SYSTEM) {
            // This is a goal generated by the Grounding system after a tool failure.
            if (focusThought.content().text().startsWith("Investigate and resolve failure")) {
                return memory.findSchemaBySymbolicName(Genesis.FAILURE_RECOVERY_SCHEMA_SYMBOL)
                        .orElse(null); // Fall through to semantic search if not found
            }
        }

        // Priority 2: For all other goals (especially from PERCEPTION), use semantic search to find the best schema.
        // This allows the system to flexibly choose between the logical action schema, web research, etc.
        if (focusThought.content().embedding() == null || focusThought.content().embedding().isEmpty()) {
            LOGGER.debug("Focus thought has no embedding, cannot do semantic search for schema.");
            return null;
        }

        List<ScoredThought> schemas = memory.retrieveSimilar(
                focusThought.content().embedding(),
                SCHEMA_COUNT,
                ThoughtType.SCHEMA
        );

        if (schemas.isEmpty()) {
            LOGGER.debug("No schemas found matching the focus thought.");
            return null;
        }

        // Find the best schema by weighting the retrieval score by the schema's clarity
        Optional<ScoredThought> bestSchema = schemas.stream()
                .max(Comparator.comparingDouble(scoredSchema ->
                        scoredSchema.score() * scoredSchema.thought().state().clarity()));

        if (bestSchema.isPresent()) {
            Thought selectedSchema = bestSchema.get().thought();
            LOGGER.info("Semantically selected schema {} with combined score of {}.",
                    selectedSchema.id(),
                    bestSchema.get().score() * selectedSchema.state().clarity());
            return selectedSchema;
        }

        return null;
    }

    private List<Thought> handleSchemaRewriteGoal(Thought rewriteGoal) {
        // The faulty schema is the first (and only) parent in the trace
        String faultySchemaId = rewriteGoal.metadata().trace().getFirst();
        Optional<Thought> faultySchemaOpt = memory.getThoughtById(faultySchemaId);

        if (faultySchemaOpt.isEmpty()) {
            LOGGER.error("Could not find faulty schema with ID {} to rewrite.", faultySchemaId);
            return List.of(createSimpleReport("Could not find faulty schema with ID " + faultySchemaId, rewriteGoal.id()));
        }

        Thought faultySchema = faultySchemaOpt.get();
        // For a more advanced implementation, we could also fetch examples of low-clarity outputs.
        // For now, just passing the schema's prompt is a good start.
        String metaPrompt = promptBuilder.buildSchemaRewritePrompt(faultySchema);

        Response<AiMessage> response = chatModel.generate(UserMessage.from(metaPrompt));
        String newSchemaProceduralContent = response.content().text();

        // Create a new schema thought based on the old one, but with the new procedural content
        Thought newSchema = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(
                        faultySchema.content().text(), // Keep the same name/description
                        faultySchema.content().symbolic(), // Keep the same symbolic name
                        null,   // embedding
                        null,   // perceptual
                        newSchemaProceduralContent, // The new, improved prompt
                        null,   // feedback
                        null    // rules
                ),
                new ThoughtState(0.75, 1.0, 1.0), // Start with a reasonably high, but not perfect, clarity
                new ThoughtMeta(
                        ThoughtType.SCHEMA,
                        ThoughtOrigin.LLM_INFERENCE,
                        List.of(rewriteGoal.id()), // Trace it back to the optimization goal
                        Instant.now()
                )
        );

        LOGGER.info("Generated new, optimized schema {} to replace {}", newSchema.id(), faultySchema.id());
        // In a full implementation, we might want to "deprecate" the old schema here.
        // For now, the new schema will simply be available and hopefully retrieved more often.
        return List.of(newSchema);
    }

    private List<Thought> handleEffortModelRewriteGoal(Thought rewriteGoal) {
        String metaPrompt = promptBuilder.buildEffortModelRewritePrompt(rewriteGoal.content().text());

        Response<AiMessage> response = chatModel.generate(UserMessage.from(metaPrompt));
        String responseText = response.content().text().trim();

        try {
            String[] parts = responseText.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Response must be two comma-separated numbers.");
            }
            double coefficient = Double.parseDouble(parts[0].trim());
            double intercept = Double.parseDouble(parts[1].trim());

            LinearTextEffortModel newModel = new LinearTextEffortModel(coefficient, intercept);

            Thought newSchema = new Thought(
                    UUID.randomUUID().toString(),
                    new ThoughtContent(
                            "An AI-generated schema for predicting computational effort based on text length.",
                            EffortModelOptimizer.REWRITE_EFFORT_MODEL_SYMBOLIC,
                            null,
                            null,
                            newModel, // The new, improved model
                            null,
                            null
                    ),
                    new ThoughtState(0.85, 1.0, 1.0), // High clarity
                    new ThoughtMeta(
                            ThoughtType.SCHEMA,
                            ThoughtOrigin.LLM_INFERENCE,
                            List.of(rewriteGoal.id()),
                            Instant.now()
                    )
            );
            LOGGER.info("Generated new, optimized effort model schema {} with parameters: coef={}, intercept={}",
                    newSchema.id(), coefficient, intercept);

            return List.of(newSchema);

        } catch (Exception e) {
            LOGGER.error("Failed to parse new effort model parameters from LLM response: '{}'", responseText, e);
            return List.of(createSimpleReport("Failed to create a new effort model. Reason: " + e.getMessage(), rewriteGoal.id()));
        }
    }
}
