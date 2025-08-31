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
import com.senars.systems.Rule;
import com.senars.systems.immemory.*;
import com.senars.systems.rules.KeywordBlocklistRule;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
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

        // 2. Foundational Systems
        IMemoryNexus memoryNexus = new InMemoryMemoryNexus();
        List<Rule> rules = List.of(
            new KeywordBlocklistRule(List.of("delete all files", "shutdown", "rm -rf"))
        );
        IGovernanceLayer governanceLayer = new InMemoryGovernanceLayer(rules);
        IGroundingSystem groundingSystem = new InMemoryGroundingSystem(memoryNexus);

        // 3. Cognitive Cycle Components
        IPerceptionSystem perceptionSystem = new StubPerceptionSystem();
        IActionSystem actionSystem = new StubActionSystem();

        // 4. Attention and Salience
        MotiveHierarchy motiveHierarchy = new MotiveHierarchy();
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
        CognitiveCycle cognitiveCycle = new CognitiveCycle(
                perceptionSystem,
                attentionFunnel,
                cognitiveProcessor,
                actionSystem,
                memoryNexus,
                governanceLayer
        );

        LOGGER.info("SeNARS Cognitive System Initialized. Starting cognitive cycle.");

        // 7. Main Loop
        int stepCount = 0;
        while (stepCount < 10) { // Run for a limited number of steps for this example
            LOGGER.info("--- Cycle Step {} ---", ++stepCount);
            cognitiveCycle.step();
            Thread.sleep(1000); // Pause between cycles
        }

        LOGGER.info("SeNARS Cognitive System finished after {} steps.", stepCount);
    }
}
