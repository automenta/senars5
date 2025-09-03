package com.senars;

import com.senars.attention.Attention;
import com.senars.attention.AttentionService;
import com.senars.attention.SalienceCalculator;
import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.cycle.Action;
import com.senars.cycle.ActionFeedbackQueue;
import com.senars.effort.LinearTextEffortModel;

import java.util.Collections;
import java.util.UUID;
import com.senars.cycle.CognitiveCycle;
import com.senars.cycle.Inference;
import com.senars.cycle.CognitiveCycleServices;
import com.senars.cycle.CompositePerception;
import com.senars.cycle.Perception;
import com.senars.cycle.PerceptionChannel;
import com.senars.cycle.ToolUsingAction;
import com.senars.db.DatabaseManager;
import com.senars.effort.EffortPredictor;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.events.LoggingEventSubscriber;
import com.senars.expansion.AutonomousCapabilityExpansion;
import com.senars.explanation.CausalExplanationGenerator;
import com.senars.explanation.ExplanationGenerator;
import com.senars.explanation.ExplanationService;
import com.senars.governance.GovernanceService;
import com.senars.health.SystemHealthMonitor;
import com.senars.lm.PromptBuilder;
import com.senars.lm.ToolKit;
import com.senars.logic.*;
import com.senars.logic.mdr.MDRService;
import com.senars.motive.MotiveHierarchy;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.systems.GoalGraph;
import com.senars.io.ConsolePerception;
import com.senars.systems.Memory;
import com.senars.systems.memory.DefaultMemory;
import com.senars.systems.perception.FilePerceptionChannel;
import com.senars.systems.rules.PreventDeprecatedSchemaUseRule;
import com.senars.tools.*;
import com.senars.ui.ConsolePrinter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.senars.systems.Rule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory for creating and wiring all components of the SeNARS system.
 * This class handles the dependency injection to make the system modular and testable.
 */
public class SystemFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemFactory.class);

    // Public fields for components that might be needed by the Main loop (e.g., for shutdown)
    public final Memory memory;
    public final EventBus eventBus;
    public final SchemaOptimizer schemaOptimizer; // Made public for test access
    public final LogicEngine logicEngine; // Made public for test access
    public final GovernanceService governance; // Made public for access to the new governance service
    public final AttentionService attentionService; // Made public for access to the new attention service
    public final SystemHealthMonitor healthMonitor; // Made public for access to the new health monitor
    public final ExplanationService explanationService; // Made public for access to the new explanation service
    public final AutonomousCapabilityExpansion capabilityExpansion; // Made public for access to the new capability expansion
    private final CognitiveCycle cognitiveCycle;

    public SystemFactory() {
        this(OllamaChatModel.builder()
                .baseUrl(AppConfig.getInstance().getLlmApiUrl())
                .modelName(AppConfig.getInstance().getLlmModelName())
                .timeout(Duration.ofSeconds(AppConfig.getInstance().getLlmApiTimeout()))
                .build());
    }

    public SystemFactory(ChatLanguageModel chatModel) {
        this(chatModel, Paths.get(AppConfig.getInstance().getGraphDbFilePath()));
    }

    public SystemFactory(ChatLanguageModel chatModel, Path dbPath) {
        // 1. Configuration
        AppConfig config = AppConfig.getInstance();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.eventBus = new EventBus();
        LoggingEventSubscriber loggingSubscriber = new LoggingEventSubscriber();
        loggingSubscriber.subscribeToAll(eventBus);

        // 2. Foundational Systems
        DatabaseManager dbManager = new DatabaseManager(dbPath);

        this.memory = new DefaultMemory(dbManager);
        this.logicEngine = new LogicEngine();

        // Create the Unified Causal Reasoner (replaces Grounding system)
        UnifiedCausalReasoner ucr = UCRFactory.createUCR(memory, eventBus, chatModel);

        // Create the new Governance Service
        LOGGER.info("Initializing Governance Layer...");
        String constitution = Genesis.loadConstitution();
        ChatLanguageModel vettingModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName()) // In a real system, this might be a smaller, faster model
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout() / 2)) // Faster timeout for safety checks
                .build();
        List<Rule> rules = new ArrayList<>();
        rules.add(new PreventDeprecatedSchemaUseRule(memory, logicEngine));

        this.governance = new GovernanceService(ucr, rules, constitution, vettingModel);
        LOGGER.info("Governance Layer initialized with unified Governance Service.");
        this.schemaOptimizer = new SchemaOptimizer(memory, eventBus);
        EffortModelOptimizer effortOptimizer = new EffortModelOptimizer(memory, eventBus);

        // 3. Genesis & Bootstrapping
        LOGGER.info("Executing Genesis Protocol...");
        if (memory.getAllThoughts().isEmpty()) {
            LOGGER.info("Memory is empty. Seeding default schemas...");
            ThoughtContent content = new ThoughtContent(
                    "Default effort prediction model based on text length.",
                    EffortPredictor.EFFORT_MODEL_SCHEMA_NAME,
                    null,
                    null,
                    new LinearTextEffortModel(0.01, 1.0), // The procedural content is the model object itself
                    null,
                    null
            );

            ThoughtMeta metadata = new ThoughtMeta(
                    ThoughtType.SCHEMA,
                    ThoughtOrigin.SYSTEM,
                    Collections.emptyList(),
                    java.time.Instant.now()
            );

            ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);

            Thought schemaThought = new Thought(
                    UUID.nameUUIDFromBytes(EffortPredictor.EFFORT_MODEL_SCHEMA_NAME.getBytes()).toString(),
                    content,
                    state,
                    metadata
            );
            memory.saveThought(schemaThought);
            LOGGER.info("Default effort model schema seeded.");
        }
        List<Thought> genesisDrives = Genesis.createGenesisDrives(embeddingModel);
        List<Thought> genesisBeliefs = Genesis.loadKnowledgeFromFile("genesis_knowledge.json", embeddingModel);
        List<Thought> genesisSchemas = Genesis.loadSchemasFromFile("genesis_schemas.json", embeddingModel);
        List<Thought> reasoningSchemas = Genesis.loadSchemasFromFile("reasoning_schemas.json", embeddingModel);
        List<Thought> embeddingSchemas = Genesis.loadSchemasFromFile("embedding-generation-schema.json", embeddingModel);
        List<Thought> optimizationSchemas = Genesis.loadSchemasFromFile("schema_optimizer_schema.json", embeddingModel);
        optimizationSchemas.addAll(Genesis.loadSchemasFromFile("effort_model_optimizer_schema.json", embeddingModel));
        List<Thought> metaCognitionSchemas = Genesis.loadSchemasFromFile("meta-cognition-schemas.json", embeddingModel);
        Thought logicalActionSchema = Genesis.createLogicalActionSchema(embeddingModel);
        Thought failureRecoverySchema = Genesis.createFailureRecoverySchema(embeddingModel);

        genesisDrives.forEach(memory::saveThought);
        genesisBeliefs.forEach(memory::saveThought);
        genesisSchemas.forEach(memory::saveThought);
        reasoningSchemas.forEach(memory::saveThought);
        embeddingSchemas.forEach(memory::saveThought);
        optimizationSchemas.forEach(memory::saveThought);
        metaCognitionSchemas.forEach(memory::saveThought);
        memory.saveThought(logicalActionSchema);
        memory.saveThought(failureRecoverySchema);
        LOGGER.info("Loaded {} Genesis Drives, {} Beliefs, and {} Schemas into Memory Nexus.", genesisDrives.size(), genesisBeliefs.size(), genesisSchemas.size() + reasoningSchemas.size() + embeddingSchemas.size() + optimizationSchemas.size() + metaCognitionSchemas.size() + 2);

        // 4. Cognitive Cycle Components
        Inference inference = new Inference(memory, logicEngine);
        ToolKit toolKit = new ToolKit(
                new SearchTools(),
                new WebTools(),
                new FileSystemTools(),
                new CodeExecutionTool(),
                new EmbeddingGenerationTool(memory, embeddingModel),
                new LogicalInferenceTool(inference),
                new SchemaManagementTool(memory, logicEngine, eventBus),
                new ApiTool()
        );
        Action action = new ToolUsingAction(toolKit);

        LOGGER.info("Initializing Perception System...");
        List<PerceptionChannel> perceptionChannels = List.of(
                new ConsolePerception(embeddingModel),
                new FilePerceptionChannel(config.getPerceptionFileDirectory(), embeddingModel)
        );
        Perception perception = new CompositePerception(perceptionChannels);
        LOGGER.info("Perception System Initialized with {} channel(s).", perceptionChannels.size());

        // 5. Attention and Salience
        MotiveHierarchy motives = new MotiveHierarchy(genesisDrives);
        Thought primeAmbition = Genesis.createPrimeAmbition(embeddingModel);
        motives.addAmbition(primeAmbition);
        memory.saveThought(primeAmbition);
        Thought researchGoal = Genesis.createResearchGoal(embeddingModel);
        memory.saveThought(researchGoal);

        LOGGER.info("--- GENESIS PROTOCOL COMPLETE ---");
        genesisDrives.forEach(drive -> LOGGER.info("Loaded Drive: {}", drive.content().text()));
        genesisBeliefs.forEach(belief -> LOGGER.info("Loaded Belief: {}", belief.content().text()));
        genesisSchemas.forEach(schema -> LOGGER.info("Loaded Schema: {}", schema.content().symbolic()));
        LOGGER.info("Established Prime Ambition: {}", primeAmbition.content().text());
        LOGGER.info("Set initial Goal: {}", researchGoal.content().text());
        LOGGER.info("---------------------------------");

        EffortPredictor effortPredictor = new EffortPredictor(memory);
        EffortTracker effortTracker = new EffortTracker(effortPredictor);
        SalienceCalculator salienceCalculator = new SalienceCalculator(effortPredictor, memory);

        // Create the new unified Attention Service
        this.attentionService = new AttentionService(memory, salienceCalculator, ucr, motives, eventBus);
        this.attentionService.addCandidate(researchGoal);


        // 6. No LLM-based Cognitive Processor anymore (replaced by UCR)

        // 7. The Cognitive Cycle itself
        ActionFeedbackQueue feedbackQueue = new ActionFeedbackQueue();
        MetaCognitiveService metaCognitiveService = new MetaCognitiveService(chatModel);
        MDRService mdrService = new MDRService(ucr, memory, chatModel);
        // Register the schema optimization monitor
        mdrService.addMonitor(new com.senars.logic.mdr.SchemaOptimizationMonitorConfig(memory));
        GoalGraph goalGraph = new GoalGraph(dbManager);
        GoalOrientedPlanner goalOrientedPlanner = new GoalOrientedPlanner(goalGraph, ucr);

        // 8. Phase 4 Components
        // Create the System Health Monitor
        this.healthMonitor = new SystemHealthMonitor(memory, ucr, eventBus);

        // Create the Explanation Service
        this.explanationService = new ExplanationService(memory, ucr, eventBus, chatModel);

        // Create the Autonomous Capability Expansion
        this.capabilityExpansion = new AutonomousCapabilityExpansion(memory, ucr, eventBus, chatModel, explanationService, healthMonitor);

        CognitiveCycleServices services = new CognitiveCycleServices(
                perception,
                this.attentionService,
                ucr,
                action,
                memory,
                governance,
                feedbackQueue,
                schemaOptimizer,
                effortOptimizer,
                effortTracker,
                eventBus,
                metaCognitiveService,
                mdrService,
                goalOrientedPlanner,
                new ConsolePrinter()
        );
        this.cognitiveCycle = new CognitiveCycle(services);

        // 9. Event Bus Subscriptions
        PromptBuilder promptBuilder = new PromptBuilder();
        ExplanationGenerator explanationGenerator = new ExplanationGenerator(eventBus);
        CausalExplanationGenerator causalExplanationGenerator = new CausalExplanationGenerator(eventBus, memory, ucr);
        eventBus.subscribe(Events.NewThoughtCreatedEvent.class, cognitiveCycle::onNewThoughtCreated);
        eventBus.subscribe(Events.NewThoughtCreatedEvent.class, causalExplanationGenerator);
        eventBus.subscribe(Events.ActionExecutedEvent.class, schemaOptimizer::onActionExecuted);
        eventBus.subscribe(Events.CognitionStartEvent.class, effortTracker::onCognitionStart);
        eventBus.subscribe(Events.CognitionEndEvent.class, effortTracker::onCognitionEnd);
        eventBus.subscribe(Events.SchemaOptimizedEvent.class, explanationGenerator);

        // Subscribe to problem reports from the health monitor
        eventBus.subscribe(Events.NewThoughtCreatedEvent.class, event -> {
            if (event.thought().metadata().type() == com.senars.core.ThoughtType.REPORT &&
                    event.thought().content().symbolic() != null &&
                    event.thought().content().symbolic().contains("health_monitor")) {
                capabilityExpansion.processSystemicProblem(event.thought());
            }
        });
    }

    public CognitiveCycle getCognitiveCycle() {
        return this.cognitiveCycle;
    }
}
