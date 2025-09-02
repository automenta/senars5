package com.senars;

import com.senars.config.AppConfig;
import com.senars.core.Genesis;
import com.senars.core.Thought;
import com.senars.cycle.*;
import com.senars.db.DatabaseManager;
import com.senars.effort.EffortPredictor;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.events.LoggingEventSubscriber;
import com.senars.lm.Langchain4JCognition;
import com.senars.lm.PromptBuilder;
import com.senars.lm.StructuredOutputParser;
import com.senars.lm.ToolKit;
import com.senars.motive.MotiveHierarchy;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Governor;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import com.senars.systems.Rule;
import com.senars.systems.immemory.*;
import com.senars.systems.perception.FilePerceptionChannel;
import com.senars.systems.rules.KeywordBlocklistRule;
import com.senars.tools.*;
import com.senars.xai.Explain;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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

    private final CognitiveCycle cognitiveCycle;

    public SystemFactory() {
        // 1. Configuration
        AppConfig config = AppConfig.getInstance();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.eventBus = new EventBus();
        LoggingEventSubscriber loggingSubscriber = new LoggingEventSubscriber();
        loggingSubscriber.subscribeToAll(eventBus);

        // 2. Foundational Systems
        Path dbPath = Paths.get(config.getGraphDbFilePath());
        DatabaseManager dbManager = new DatabaseManager(dbPath);

        this.memory = new InMemoryMemory(config, dbManager);

        // Create the two-stage governor
        LOGGER.info("Initializing Governance Layer...");
        String constitution = Genesis.loadConstitution();
        OllamaChatModel vettingModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName()) // In a real system, this might be a smaller, faster model
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout() / 2)) // Faster timeout for safety checks
                .build();

        List<Rule> rules = List.of(
                new KeywordBlocklistRule(List.of("delete all files", "shutdown", "rm -rf"))
        );
        Governor governance = new InMemoryGovernor(rules, constitution, vettingModel);
        LOGGER.info("Governance Layer initialized with {} rules and constitutional vetting.", rules.size());


        Grounding grounding = new InMemoryGrounding(memory, eventBus);
        SchemaOptimizer schemaOptimizer = new SchemaOptimizer(memory, eventBus);
        EffortModelOptimizer effortOptimizer = new EffortModelOptimizer(memory, eventBus);

        // 3. Genesis & Bootstrapping
        LOGGER.info("Executing Genesis Protocol...");
        List<Thought> genesisDrives = Genesis.createGenesisDrives(embeddingModel);
        List<Thought> genesisBeliefs = Genesis.loadKnowledgeFromFile("genesis_knowledge.json", embeddingModel);
        List<Thought> genesisSchemas = Genesis.loadSchemasFromFile("genesis_schemas.json", embeddingModel);
        List<Thought> reasoningSchemas = Genesis.loadSchemasFromFile("reasoning_schemas.json", embeddingModel);
        List<Thought> parsingSchemas = Genesis.loadSchemasFromFile("parsing-schema.json", embeddingModel);
        List<Thought> embeddingSchemas = Genesis.loadSchemasFromFile("embedding-generation-schema.json", embeddingModel);
        Thought logicalActionSchema = Genesis.createLogicalActionSchema(embeddingModel);
        Thought failureRecoverySchema = Genesis.createFailureRecoverySchema(embeddingModel);

        genesisDrives.forEach(memory::saveThought);
        genesisBeliefs.forEach(memory::saveThought);
        genesisSchemas.forEach(memory::saveThought);
        reasoningSchemas.forEach(memory::saveThought);
        parsingSchemas.forEach(memory::saveThought);
        embeddingSchemas.forEach(memory::saveThought);
        memory.saveThought(logicalActionSchema);
        memory.saveThought(failureRecoverySchema);
        LOGGER.info("Loaded {} Genesis Drives, {} Beliefs, and {} Schemas into Memory Nexus.", genesisDrives.size(), genesisBeliefs.size(), genesisSchemas.size() + reasoningSchemas.size() + parsingSchemas.size() + embeddingSchemas.size() + 2);

        // 4. Cognitive Cycle Components
        ToolKit toolKit = new ToolKit(
                new SearchTools(),
                new WebTools(),
                new FileSystemTools(),
                new CodeExecutionTool(),
                new EmbeddingGenerationTool(memory, embeddingModel),
                new LogicalInferenceTool(memory)
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
        SalienceCalculator salienceCalculator = new SalienceCalculator(effortPredictor);
        Attention attention = new SalienceAttention(salienceCalculator, motives, eventBus);
        attention.addCandidate(researchGoal);

        // 6. LLM-based Cognitive Processor
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName())
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout()))
                .build();

        PromptBuilder promptBuilder = new PromptBuilder();
        StructuredOutputParser outputParser = new StructuredOutputParser();
        Explain explain = new Explain(memory);

        Cognition cognitiveProcessor = new Langchain4JCognition(
                chatModel,
                memory,
                promptBuilder,
                outputParser,
                explain,
                toolKit,
                eventBus
        );

        // 7. The Cognitive Cycle itself
        ActionFeedbackQueue feedbackQueue = new ActionFeedbackQueue();
        this.cognitiveCycle = new CognitiveCycle(
                perception,
                attention,
                cognitiveProcessor,
                action,
                memory,
                governance,
                grounding,
                feedbackQueue,
                schemaOptimizer,
                effortOptimizer,
                effortTracker,
                eventBus
        );

        // 8. Event Bus Subscriptions
        eventBus.subscribe(Events.NewThoughtCreatedEvent.class, cognitiveCycle::onNewThoughtCreated);
        eventBus.subscribe(Events.ActionExecutedEvent.class, schemaOptimizer::onActionExecuted);
        eventBus.subscribe(Events.CognitionStartEvent.class, effortTracker::onCognitionStart);
        eventBus.subscribe(Events.CognitionEndEvent.class, effortTracker::onCognitionEnd);
    }

    public CognitiveCycle getCognitiveCycle() {
        return this.cognitiveCycle;
    }
}
