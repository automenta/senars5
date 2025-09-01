package com.senars.cycle;

import com.senars.core.SessionManager;
import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMetadata;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.systems.IGovernanceLayer;
import com.senars.systems.IGroundingSystem;
import com.senars.systems.IMemoryNexus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The core orchestrator of the SeNARS cognitive architecture.
 * It runs the main perception-attention-processing loop.
 */
public class CognitiveCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitiveCycle.class);

    private final IPerceptionSystem perceptionSystem;
    private final IAttentionFunnel attentionFunnel;
    private final ICognitiveProcessor cognitiveProcessor;
    private final IActionSystem actionSystem;
    private final IMemoryNexus memoryNexus;
    private final IGovernanceLayer governanceLayer;
    private final SessionManager sessionManager;
    private final IGroundingSystem groundingSystem;

    public CognitiveCycle(
        IPerceptionSystem perceptionSystem,
        IAttentionFunnel attentionFunnel,
        ICognitiveProcessor cognitiveProcessor,
        IActionSystem actionSystem,
        IMemoryNexus memoryNexus,
        IGovernanceLayer governanceLayer,
        SessionManager sessionManager,
        IGroundingSystem groundingSystem
    ) {
        this.perceptionSystem = Objects.requireNonNull(perceptionSystem);
        this.attentionFunnel = Objects.requireNonNull(attentionFunnel);
        this.cognitiveProcessor = Objects.requireNonNull(cognitiveProcessor);
        this.actionSystem = Objects.requireNonNull(actionSystem);
        this.memoryNexus = Objects.requireNonNull(memoryNexus);
        this.governanceLayer = Objects.requireNonNull(governanceLayer);
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.groundingSystem = Objects.requireNonNull(groundingSystem);
    }

    /**
     * Executes a single step of the cognitive cycle with robust error handling.
     */
    public void step() {
        try {
            // 1. Perception Stage
            List<Thought> perceivedThoughts = perceptionSystem.perceive();
            if (!perceivedThoughts.isEmpty()) {
                LOGGER.info("Perceived {} new thoughts.", perceivedThoughts.size());
                for (Thought thought : perceivedThoughts) {
                    // Check if the thought is a feedback report
                    if (thought.metadata().type() == ThoughtType.REPORT && thought.content().feedback() != null) {
                        processFeedbackReport(thought);
                    } else {
                        attentionFunnel.addCandidate(thought);
                    }
                }
            }

            // 2. Prioritization Stage
            Optional<Thought> focusThoughtOpt = attentionFunnel.selectFocusThought();

            if (focusThoughtOpt.isEmpty()) {
                LOGGER.debug("No focus thought. System is idle.");
                return;
            }

            Thought focusThought = focusThoughtOpt.get();
            LOGGER.info("Focusing on thought: {}", focusThought.id());

            // 3. Processing Stage
            List<Thought> newThoughts = cognitiveProcessor.process(focusThought);

            for (Thought newThought : newThoughts) {
                handleNewThought(newThought);
            }
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during the cognitive cycle.", e);
            // In a more advanced implementation, this could trigger a system-level
            // goal to diagnose the failure. For now, we log and continue.
        }
    }

    private void handleNewThought(Thought thought) {
        LOGGER.info("New thought generated: {} - {}", thought.metadata().type(), thought.id());
        memoryNexus.saveThought(thought);

        if (thought.metadata().type() == ThoughtType.ACTION_PLAN) {
            try {
                Optional<String> vetoReason = governanceLayer.reviewPlan(thought);
                if (vetoReason.isPresent()) {
                    LOGGER.warn("ACTION_PLAN vetoed: {}", vetoReason.get());
                    createReplanGoal(thought, vetoReason.get());
                } else {
                    LOGGER.info("ACTION_PLAN approved. Executing...");
                    sessionManager.setLastActionPlan(thought); // Track the action being executed
                    actionSystem.executePlan(thought);
                }
            } catch (Exception e) {
                LOGGER.error("Error during action plan review or execution for thought: {}", thought.id(), e);
            }
        } else {
            attentionFunnel.addCandidate(thought);
        }
    }

    private void createReplanGoal(Thought vetoedPlan, String reason) {
        String newId = UUID.randomUUID().toString();
        Thought replanGoal = new Thought(
            newId,
            new ThoughtContent(
                "Reformulate plan " + vetoedPlan.id() + " due to safety violation: " + reason,
                null, null, null, null, null
            ),
            new ThoughtState(1.0, 100.0, 1.0), // High clarity, salience, and activation
            new ThoughtMetadata(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                List.of(vetoedPlan.id()),
                Instant.now()
            )
        );
        LOGGER.info("Created replan goal: {}", replanGoal.id());
        memoryNexus.saveThought(replanGoal);
        attentionFunnel.addCandidate(replanGoal);
    }

    private void processFeedbackReport(Thought feedbackReport) {
        Optional<Thought> lastActionOpt = sessionManager.getLastActionPlan();
        if (lastActionOpt.isEmpty()) {
            LOGGER.warn("Received feedback report {} but there is no last action plan in the session to attribute it to. Ignoring.", feedbackReport.id());
            return;
        }

        Thought lastAction = lastActionOpt.get();
        LOGGER.info("Attributing feedback report {} to last action plan {}", feedbackReport.id(), lastAction.id());

        // Create a new, enriched feedback report with the trace from the action it's for.
        Thought enrichedReport = new Thought(
                feedbackReport.id(),
                feedbackReport.content(),
                feedbackReport.state(),
                new ThoughtMetadata(
                        feedbackReport.metadata().type(),
                        feedbackReport.metadata().origin(),
                        lastAction.metadata().trace(), // The crucial link!
                        feedbackReport.metadata().timestamp()
                )
        );

        groundingSystem.processFeedback(enrichedReport);
        memoryNexus.saveThought(enrichedReport); // Save the enriched report for provenance
        sessionManager.clearLastActionPlan(); // Clear the session to prevent re-attributing feedback
    }
}
