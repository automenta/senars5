package com.senars.explanation;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.EventSubscriber;
import com.senars.events.Events;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An event subscriber that listens for explanation requests and
 * generates rich, causal narratives using the UCR.
 */
public class CausalExplanationGenerator implements EventSubscriber<Events.NewThoughtCreatedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CausalExplanationGenerator.class);
    private static final double EXPLANATION_SALIENCE = 90.0; // High priority

    private final EventBus eventBus;
    private final Memory memory;
    private final UnifiedCausalReasoner ucr;
    private final CausalChainTracer tracer;

    public CausalExplanationGenerator(EventBus eventBus, Memory memory, UnifiedCausalReasoner ucr) {
        this.eventBus = eventBus;
        this.memory = memory;
        this.ucr = ucr;
        this.tracer = new CausalChainTracer(memory);
    }

    @Override
    public void onEvent(Events.NewThoughtCreatedEvent event) {
        Thought thought = event.thought();

        // Check if this is an explanation request (EXPLAIN type thought)
        if (thought.metadata().type() == ThoughtType.EXPLAIN) {
            LOGGER.info("Received explanation request for thought: {}", thought.id());
            generateCausalExplanation(thought);
        }
    }

    /**
     * Generates a rich, causal explanation for a target thought.
     *
     * @param explanationRequest The explanation request thought.
     */
    private void generateCausalExplanation(Thought explanationRequest) {
        try {
            // Extract the target thought ID from the request
            List<String> trace = explanationRequest.metadata().trace();
            if (trace == null || trace.isEmpty()) {
                LOGGER.warn("Explanation request has no target thought ID in trace.");
                return;
            }

            String targetThoughtId = trace.getFirst();
            memory.getThoughtById(targetThoughtId).ifPresentOrElse(
                    targetThought -> {
                        // Generate the causal explanation
                        String explanationText = generateExplanationText(targetThought);

                        // Create a report thought with the explanation
                        ThoughtContent content = new ThoughtContent(explanationText, "xai:causal_explanation", null, null, null, null, null);
                        ThoughtMeta meta = new ThoughtMeta(
                                ThoughtType.REPORT,
                                ThoughtOrigin.SYSTEM,
                                List.of(targetThoughtId),
                                Instant.now()
                        );
                        ThoughtState state = new ThoughtState(1.0, EXPLANATION_SALIENCE, 1.0);
                        Thought explanationReport = new Thought(UUID.randomUUID().toString(), content, state, meta);

                        LOGGER.info("Generated causal explanation report: {}", explanationReport.id());
                        eventBus.publish(new Events.NewThoughtCreatedEvent(explanationReport));
                    },
                    () -> LOGGER.warn("Target thought not found in memory: {}", targetThoughtId)
            );
        } catch (Exception e) {
            LOGGER.error("Error generating causal explanation", e);
        }
    }

    /**
     * Generates the explanation text using causal chain analysis.
     *
     * @param targetThought The target thought to explain.
     * @return A formatted explanation text.
     */
    private String generateExplanationText(Thought targetThought) {
        try {
            // Get the causal chain
            List<Thought> causalChain = tracer.getCausalChain(targetThought);

            // Format the causal chain into a narrative
            return tracer.formatCausalChain(causalChain, targetThought);
        } catch (Exception e) {
            LOGGER.error("Error generating explanation text", e);
            return "Error generating explanation: " + e.getMessage();
        }
    }
}