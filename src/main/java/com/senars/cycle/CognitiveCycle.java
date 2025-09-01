package com.senars.cycle;

import com.senars.core.*;
import com.senars.systems.Governor;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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

    private final Perception perception;
    private final Attention attention;
    private final Cognition cognition;
    private final Action action;
    private final Memory memory;
    private final Governor governor;
    private final Sessions sessions;
    private final Grounding grounding;

    public CognitiveCycle(
            Perception perception,
            Attention attention,
            Cognition cognition,
            Action action,
            Memory memory,
            Governor governor,
            Sessions sessions,
            Grounding grounding
    ) {
        this.perception = Objects.requireNonNull(perception);
        this.attention = Objects.requireNonNull(attention);
        this.cognition = Objects.requireNonNull(cognition);
        this.action = Objects.requireNonNull(action);
        this.memory = Objects.requireNonNull(memory);
        this.governor = Objects.requireNonNull(governor);
        this.sessions = Objects.requireNonNull(sessions);
        this.grounding = Objects.requireNonNull(grounding);
    }

    /**
     * Executes a single step of the cognitive cycle with robust error handling.
     */
    public void step() {
        try {
            // 1. Perception Stage
            List<Thought> perceivedThoughts = perception.perceive();
            if (!perceivedThoughts.isEmpty()) {
                LOGGER.info("Perceived {} new thoughts.", perceivedThoughts.size());
                for (Thought thought : perceivedThoughts) {
                    // Check if the thought is a feedback report
                    if (thought.metadata().type() == ThoughtType.REPORT && thought.content().feedback() != null) {
                        processFeedbackReport(thought);
                    } else {
                        attention.addCandidate(thought);
                    }
                }
            }

            // 2. Prioritization Stage
            Optional<Thought> focusThoughtOpt = attention.selectFocusThought();

            if (focusThoughtOpt.isEmpty()) {
                LOGGER.debug("No focus thought. System is idle.");
                return;
            }

            Thought focusThought = focusThoughtOpt.get();
            LOGGER.info("Focusing on thought: {}", focusThought.id());

            // 3. Processing Stage
            List<Thought> newThoughts = cognition.process(focusThought);

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
        memory.saveThought(thought);

        if (thought.metadata().type() == ThoughtType.ACTION_PLAN) {
            handleActionPlan(thought);
        } else {
            // Check if this is a report generated from an explanation request
            if (thought.metadata().type() == ThoughtType.REPORT) {
                isExplanationReport(thought).ifPresent(isExplanation -> {
                    if (isExplanation) {
                        printExplanation(thought);
                    }
                });
            }
            attention.addCandidate(thought);
        }
    }

    private void handleActionPlan(Thought thought) {
        try {
            Optional<String> vetoReason = governor.reviewPlan(thought);
            if (vetoReason.isPresent()) {
                LOGGER.warn("ACTION_PLAN vetoed: {}", vetoReason.get());
                createReplanGoal(thought, vetoReason.get());
            } else {
                LOGGER.info("ACTION_PLAN approved. Executing...");
                sessions.setLastActionPlan(thought); // Track the action being executed
                action.executePlan(thought);
            }
        } catch (Exception e) {
            LOGGER.error("Error during action plan review or execution for thought: {}", thought.id(), e);
        }
    }

    private Optional<Boolean> isExplanationReport(Thought report) {
        List<String> trace = report.metadata().trace();
        if (trace == null || trace.isEmpty()) {
            return Optional.of(false);
        }
        // Check if the report traces back to an explanation request
        return memory.getThoughtById(trace.getFirst())
                .map(originatingThought -> originatingThought.metadata().type() == ThoughtType.EXPLANATION_REQUEST);
    }

    private void printExplanation(Thought report) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("🤖 EXPLANATION");
        System.out.println("----------------------------------------");
        System.out.println(report.content().text());
        System.out.println("========================================");
        System.out.println();
        System.out.print("> "); // Re-print the prompt
    }

    private void createReplanGoal(Thought vetoedPlan, String reason) {
        String newId = UUID.randomUUID().toString();
        Thought replanGoal = new Thought(
                newId,
                new ThoughtContent(
                        "Reformulate plan " + vetoedPlan.id() + " due to safety violation: " + reason,
                        null, null, null, null, null, null
                ),
                new ThoughtState(1.0, 100.0, 1.0), // High clarity, salience, and activation
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.SYSTEM,
                        List.of(vetoedPlan.id()),
                        Instant.now()
                )
        );
        LOGGER.info("Created replan goal: {}", replanGoal.id());
        memory.saveThought(replanGoal);
        attention.addCandidate(replanGoal);
    }

    private void processFeedbackReport(Thought feedbackReport) {
        Optional<Thought> lastActionOpt = sessions.getLastActionPlan();
        if (lastActionOpt.isEmpty()) {
            LOGGER.warn("Received feedback report {} but there is no last action plan in the session to attribute it to. Ignoring.", feedbackReport.id());
            return;
        }

        Thought lastAction = lastActionOpt.get();
        LOGGER.info("Attributing feedback report {} to last action plan {}", feedbackReport.id(), lastAction.id());

        // Create a new, enriched feedback report with the trace from the action it's for.
        var feedbackMeta = feedbackReport.metadata();
        Thought enrichedReport = new Thought(
                feedbackReport.id(),
                feedbackReport.content(),
                feedbackReport.state(),
                new ThoughtMeta(
                        feedbackMeta.type(),
                        feedbackMeta.origin(),
                        lastAction.metadata().trace(), // The crucial link!
                        feedbackMeta.timestamp()
                )
        );

        grounding.processFeedback(enrichedReport);
        memory.saveThought(enrichedReport); // Save the enriched report for provenance
        sessions.clearLastActionPlan(); // Clear the session to prevent re-attributing feedback
    }
}
