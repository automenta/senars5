package com.senars;

import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.cycle.*;
import com.senars.effort.EffortPredictor;
import com.senars.llm.Langchain4jCognitiveProcessor;
import com.senars.llm.PromptBuilder;
import com.senars.llm.StructuredOutputParser;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.IGovernanceLayer;
import com.senars.systems.IGroundingSystem;
import com.senars.systems.IMemoryNexus;
import com.senars.systems.PersistentMemoryNexus;
import com.senars.systems.Rule;
import com.senars.systems.immemory.ConsoleActionSystem;
import com.senars.systems.immemory.ConsolePerceptionSystem;
import com.senars.systems.immemory.InMemoryGovernanceLayer;
import com.senars.systems.immemory.InMemoryGroundingSystem;
import com.senars.systems.immemory.ShutdownException;
import com.senars.systems.rules.KeywordBlocklistRule;
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

        // 2. Foundational Systems
        PersistentMemoryNexus memoryNexus = new PersistentMemoryNexus(
                config.getGraphDbFilePath(),
                config.getVectorStoreFilePath(),
                embeddingModel
        );
        List<Rule> rules = List.of(
            new KeywordBlocklistRule(List.of("delete all files", "shutdown", "rm -rf"))
        );
        IGovernanceLayer governanceLayer = new InMemoryGovernanceLayer(rules);
        IGroundingSystem groundingSystem = new InMemoryGroundingSystem(memoryNexus);

        // 3. Genesis & Bootstrapping
        LOGGER.info("Executing Genesis Protocol...");
        List<Thought> genesisDrives = Genesis.createGenesisDrives(embeddingModel);
        List<Thought> genesisBeliefs = Genesis.loadKnowledgeFromFile("genesis_knowledge.json", embeddingModel);

        genesisDrives.forEach(memoryNexus::saveThought);
        genesisBeliefs.forEach(memoryNexus::saveThought);
        LOGGER.info("Loaded {} Genesis Drives and {} Genesis Beliefs into Memory Nexus.", genesisDrives.size(), genesisBeliefs.size());


        // 4. Cognitive Cycle Components
        IPerceptionSystem perceptionSystem = new ConsolePerceptionSystem(embeddingModel);
        IActionSystem actionSystem = new ConsoleActionSystem();

        // 5. Attention and Salience
        MotiveHierarchy motiveHierarchy = new MotiveHierarchy(genesisDrives);

        // Establish Prime Ambition
        Thought primeAmbition = Genesis.createPrimeAmbition(embeddingModel);
        motiveHierarchy.addAmbition(primeAmbition);
        memoryNexus.saveThought(primeAmbition);

        LOGGER.info("--- GENESIS PROTOCOL COMPLETE ---");
        genesisDrives.forEach(drive -> LOGGER.info("Loaded Drive: {}", drive.content().text()));
        genesisBeliefs.forEach(belief -> LOGGER.info("Loaded Belief: {}", belief.content().text()));
        LOGGER.info("Established Prime Ambition: {}", primeAmbition.content().text());
        LOGGER.info("---------------------------------");

        EffortPredictor effortPredictor = new EffortPredictor(memoryNexus);
        SalienceCalculator salienceCalculator = new SalienceCalculator(effortPredictor);
        IAttentionFunnel attentionFunnel = new SalienceBasedAttentionFunnel(salienceCalculator, motiveHierarchy);

        // 5. LLM-based Cognitive Processor
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName())
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout()))
                .build();

        PromptBuilder promptBuilder = new PromptBuilder();
        StructuredOutputParser outputParser = new StructuredOutputParser();

        ICognitiveProcessor cognitiveProcessor = new Langchain4jCognitiveProcessor(
                chatModel,
                memoryNexus,
                promptBuilder,
                outputParser
        );

        // 6. The Cognitive Cycle itself
        SessionManager sessionManager = new SessionManager();
        CognitiveCycle cognitiveCycle = new CognitiveCycle(
                perceptionSystem,
                attentionFunnel,
                cognitiveProcessor,
                actionSystem,
                memoryNexus,
                governanceLayer,
                sessionManager,
                groundingSystem
        );

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
            memoryNexus.persist();
            LOGGER.info("SeNARS Cognitive System finished after {} steps.", stepCount);
        }
    }
}
