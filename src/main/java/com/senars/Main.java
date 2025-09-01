package com.senars;

import com.senars.config.AppConfig;
import com.senars.core.Genesis;
import com.senars.core.Sessions;
import com.senars.core.Thought;
import com.senars.cycle.*;
import com.senars.effort.EffortPredictor;
import com.senars.llm.Langchain4JCognition;
import com.senars.llm.PromptBuilder;
import com.senars.llm.StructuredOutputParser;
import com.senars.motive.MotiveHierarchy;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Governor;
import com.senars.systems.Grounding;
import com.senars.systems.PersistentMemory;
import com.senars.systems.Rule;
import com.senars.systems.immemory.*;
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
        PersistentMemory memory = new PersistentMemory(
                config.getGraphDbFilePath(),
                config.getVectorStoreFilePath(),
                embeddingModel
        );
        List<Rule> rules = List.of(
                new KeywordBlocklistRule(List.of("delete all files", "shutdown", "rm -rf"))
        );
        Governor governance = new InMemoryGovernor(rules);
        Grounding grounding = new InMemoryGrounding(memory);

        // 3. Genesis & Bootstrapping
        LOGGER.info("Executing Genesis Protocol...");
        List<Thought> genesisDrives = Genesis.createGenesisDrives(embeddingModel);
        List<Thought> genesisBeliefs = Genesis.loadKnowledgeFromFile("genesis_knowledge.json", embeddingModel);

        genesisDrives.forEach(memory::saveThought);
        genesisBeliefs.forEach(memory::saveThought);
        LOGGER.info("Loaded {} Genesis Drives and {} Genesis Beliefs into Memory Nexus.", genesisDrives.size(), genesisBeliefs.size());


        // 4. Cognitive Cycle Components
        Perception perception = new ConsolePerception(embeddingModel);
        Action action = new ConsoleAction();

        // 5. Attention and Salience
        MotiveHierarchy motives = new MotiveHierarchy(genesisDrives);

        // Establish Prime Ambition
        Thought primeAmbition = Genesis.createPrimeAmbition(embeddingModel);
        motives.addAmbition(primeAmbition);
        memory.saveThought(primeAmbition);

        LOGGER.info("--- GENESIS PROTOCOL COMPLETE ---");
        genesisDrives.forEach(drive -> LOGGER.info("Loaded Drive: {}", drive.content().text()));
        genesisBeliefs.forEach(belief -> LOGGER.info("Loaded Belief: {}", belief.content().text()));
        LOGGER.info("Established Prime Ambition: {}", primeAmbition.content().text());
        LOGGER.info("---------------------------------");

        EffortPredictor effortPredictor = new EffortPredictor(memory);
        SalienceCalculator salienceCalculator = new SalienceCalculator(effortPredictor);
        Attention attention = new SalienceBasedAttention(salienceCalculator, motives);

        // 5. LLM-based Cognitive Processor
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(config.getLlmApiUrl())
                .modelName(config.getLlmModelName())
                .timeout(Duration.ofSeconds(config.getLlmApiTimeout()))
                .build();

        PromptBuilder promptBuilder = new PromptBuilder();
        StructuredOutputParser outputParser = new StructuredOutputParser();

        Cognition cognitiveProcessor = new Langchain4JCognition(
                chatModel,
                memory,
                promptBuilder,
                outputParser
        );

        // 6. The Cognitive Cycle itself
        Sessions sessions = new Sessions();
        CognitiveCycle cognitiveCycle = new CognitiveCycle(
                perception,
                attention,
                cognitiveProcessor,
                action,
                memory,
                governance,
                sessions,
                grounding
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
            memory.persist();
            LOGGER.info("SeNARS Cognitive System finished after {} steps.", stepCount);
        }
    }
}
