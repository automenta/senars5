package com.senars;

import com.senars.config.AppConfig;
import com.senars.core.Genesis;
import com.senars.core.Sessions;
import com.senars.core.Thought;
import com.senars.cycle.*;
import com.senars.effort.EffortPredictor;
import com.senars.db.DatabaseManager;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.events.LoggingEventSubscriber;
import com.senars.llm.Langchain4JCognition;
import com.senars.llm.PromptBuilder;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.llm.StructuredOutputParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.senars.llm.ToolKit;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Governor;
import com.senars.tools.*;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import com.senars.systems.Rule;
import com.senars.cycle.ToolUsingAction;
import com.senars.systems.immemory.*;
import com.senars.systems.perception.FilePerceptionChannel;
import com.senars.systems.rules.KeywordBlocklistRule;
import com.senars.xai.Explain;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * The main entry point for the SeNARS Cognitive System application.
 * This class is responsible for instantiating and wiring together all the
 * necessary components of the cognitive architecture, and then starting the
 * main cognitive cycle loop.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOGGER.info("Starting SeNARS Cognitive System...");

        // 1. Configuration
        AppConfig config = AppConfig.getInstance();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        EventBus eventBus = new EventBus();
        LoggingEventSubscriber loggingSubscriber = new LoggingEventSubscriber();
        loggingSubscriber.subscribeToAll(eventBus);

        // 2. Foundational Systems
        Path dbPath = Paths.get(config.getGraphDbFilePath());
        DatabaseManager dbManager = new DatabaseManager(dbPath);

        Memory memory = new InMemoryMemory(config, dbManager);
        List<Rule> rules = List.of(
                new KeywordBlocklistRule(List.of("delete all files", "shutdown", "rm -rf"))
        );
        Governor governance = new InMemoryGovernor(rules);
        Grounding grounding = new InMemoryGrounding(memory, eventBus);
        SchemaOptimizer schemaOptimizer = new SchemaOptimizer(memory, eventBus);

        // 3. Genesis & Bootstrapping
        LOGGER.info("Executing Genesis Protocol...");
        List<Thought> genesisDrives = Genesis.createGenesisDrives(embeddingModel);
        List<Thought> genesisBeliefs = Genesis.loadKnowledgeFromFile("genesis_knowledge.json", embeddingModel);
        List<Thought> genesisSchemas = Genesis.loadSchemasFromFile("genesis_schemas.json", embeddingModel);
        List<Thought> reasoningSchemas = Genesis.loadSchemasFromFile("reasoning_schemas.json", embeddingModel);
        List<Thought> parsingSchemas = Genesis.loadSchemasFromFile("parsing-schema.json", embeddingModel);
        List<Thought> embeddingSchemas = Genesis.loadSchemasFromFile("embedding-generation-schema.json", embeddingModel);


        genesisDrives.forEach(memory::saveThought);
        genesisBeliefs.forEach(memory::saveThought);
        genesisSchemas.forEach(memory::saveThought);
        reasoningSchemas.forEach(memory::saveThought);
        parsingSchemas.forEach(memory::saveThought);
        embeddingSchemas.forEach(memory::saveThought);
        LOGGER.info("Loaded {} Genesis Drives, {} Beliefs, and {} Schemas into Memory Nexus.", genesisDrives.size(), genesisBeliefs.size(), genesisSchemas.size() + reasoningSchemas.size() + parsingSchemas.size() + embeddingSchemas.size());


        // 4. Cognitive Cycle Components
        ToolKit toolKit = new ToolKit(
                new SearchTools(),
                new WebTools(),
                new FileSystemTools(),
                new CodeExecutionTool(),
                new EmbeddingGenerationTool(memory, embeddingModel)
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

        // Establish Prime Ambition
        Thought primeAmbition = Genesis.createPrimeAmbition(embeddingModel);
        motives.addAmbition(primeAmbition);
        memory.saveThought(primeAmbition);

        // Add a more ambitious goal to test the new tool-use capabilities
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
        SalienceCalculator salienceCalculator = new SalienceCalculator(effortPredictor);
        Attention attention = new SalienceBasedAttention(salienceCalculator, motives, eventBus);
        attention.addCandidate(researchGoal); // Ensure the new goal is considered on the first cycle

        // 5. LLM-based Cognitive Processor
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName())
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout()))
                .build();

        PromptBuilder promptBuilder = new PromptBuilder();
        StructuredOutputParser outputParser = new StructuredOutputParser();
        Sessions sessions = new Sessions();
        Explain explain = new Explain(memory);

        Inference inference = new Inference(memory);
        Cognition cognitiveProcessor = new Langchain4JCognition(
                chatModel,
                memory,
                promptBuilder,
                outputParser,
                sessions,
                explain,
                inference,
                toolKit
        );

        // 6. The Cognitive Cycle itself
        ActionFeedbackQueue feedbackQueue = new ActionFeedbackQueue();
        CognitiveCycle cognitiveCycle = new CognitiveCycle(
                perception,
                attention,
                cognitiveProcessor,
                action,
                memory,
                governance,
                sessions,
                grounding,
                feedbackQueue,
                schemaOptimizer,
                eventBus
        );

        // Subscribe the cognitive cycle to events it needs to handle directly
        eventBus.subscribe(Events.NewThoughtCreatedEvent.class, cognitiveCycle::onNewThoughtCreated);
        eventBus.subscribe(Events.ActionExecutedEvent.class, schemaOptimizer::onActionExecuted);

        LOGGER.info("SeNARS Cognitive System Initialized. Starting cognitive cycle.");

        // 7. Main Loop
        long stepCount = 0;
        try {
            while (true) {
                try {
                    LOGGER.info("--- Cycle Step {} ---", ++stepCount);
                    cognitiveCycle.step();
                    Thread.sleep(200); // Pause between cycles
                } catch (ShutdownException e) {
                    LOGGER.info("Shutdown command received. Terminating SeNARS.");
                    break;
                }
            }
        } finally {
            LOGGER.info("Persisting memory state...");
            memory.persist();
            eventBus.shutdown();
            LOGGER.info("SeNARS Cognitive System finished after {} steps.", stepCount);
        }
    }
}
