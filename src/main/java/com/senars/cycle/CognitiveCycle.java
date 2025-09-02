package com.senars.cycle;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.optimizer.SchemaOptimizer;
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
    private final ActionFeedbackQueue feedbackQueue;
    private final SchemaOptimizer schemaOptimizer;
    private final EventBus eventBus;
    private long cycleCount = 0;
    private static final long OPTIMIZER_RUN_INTERVAL = 50;
    private static final int FOCUS_HISTORY_WINDOW = 10;
    private static final double FOCUS_HISTORY_REPETITION_THRESHOLD = 0.3; // If 30% of recent thoughts are the same, we might be looping
    private final List<String> focusHistory = new ArrayList<>();


    public CognitiveCycle(
            Perception perception,
            Attention attention,
            Cognition cognition,
            Action action,
            Memory memory,
            Governor governor,
            Sessions sessions,
            Grounding grounding,
            ActionFeedbackQueue feedbackQueue,
            SchemaOptimizer schemaOptimizer,
            EventBus eventBus
    ) {
        this.perception = Objects.requireNonNull(perception);
        this.attention = Objects.requireNonNull(attention);
        this.cognition = Objects.requireNonNull(cognition);
        this.action = Objects.requireNonNull(action);
        this.memory = Objects.requireNonNull(memory);
        this.governor = Objects.requireNonNull(governor);
        this.sessions = Objects.requireNonNull(sessions);
        this.grounding = Objects.requireNonNull(grounding);
        this.feedbackQueue = Objects.requireNonNull(feedbackQueue);
        this.schemaOptimizer = Objects.requireNonNull(schemaOptimizer);
        this.eventBus = Objects.requireNonNull(eventBus);
    }

    /**
     * Executes a single step of the cognitive cycle with robust error handling.
     */
    public void step() {
        try {
            cycleCount++;

            // 1. Run Schema Optimizer periodically
            if (cycleCount % OPTIMIZER_RUN_INTERVAL == 0) {
                runSchemaOptimizer();
            }

            // 2. Grounding Stage: Process feedback from previous actions
            processActionFeedback();

            // 3. Perception Stage
            List<Thought> perceivedThoughts = perception.perceive();
            if (!perceivedThoughts.isEmpty()) {
                LOGGER.info("Perceived {} new thoughts from external sources.", perceivedThoughts.size());
                perceivedThoughts.forEach(this::handleNewThought);
            }

            // 4. Prioritization Stage
            Optional<Thought> focusThoughtOpt = attention.selectFocusThought();

            if (focusThoughtOpt.isEmpty()) {
                LOGGER.debug("No focus thought. System is idle.");
                return;
            }

            Thought focusThought = focusThoughtOpt.get();
            LOGGER.info("Focusing on thought: {} - {}", focusThought.metadata().type(), focusThought.id());

            // 5. Meta-Cognition Stage: Check for loops and intervene if necessary
            focusThought = detectAndHandleCognitiveLoop(focusThought).orElse(focusThought);


            // 6. Processing Stage
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
        eventBus.publish(new Events.NewThoughtCreatedEvent(thought));

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
                String reason = vetoReason.get();
                LOGGER.warn("ACTION_PLAN vetoed: {}", reason);
                eventBus.publish(new Events.ActionPlanVetoedEvent(thought, reason));
                createReplanGoal(thought, reason);
            } else {
                LOGGER.info("ACTION_PLAN approved. Executing...");
                eventBus.publish(new Events.ActionPlanApprovedEvent(thought));
                Feedback feedback = action.executePlan(thought);
                feedbackQueue.add(feedback);
                eventBus.publish(new Events.ActionExecutedEvent(feedback)); // Publish for optimizer
            }
        } catch (Exception e) {
            LOGGER.error("Error during action plan review or execution for thought: {}", thought.id(), e);
        }
    }

    private void processActionFeedback() {
        Feedback feedback = feedbackQueue.poll();
        if (feedback != null) {
            grounding.processFeedback(feedback);
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
        handleNewThought(replanGoal); // Use handleNewThought to ensure it's saved and added to attention
    }

    private void runSchemaOptimizer() {
        LOGGER.info("Cognitive cycle {} reached. Running schema optimizer.", cycleCount);
        List<Thought> optimizationGoals = schemaOptimizer.run();
        if (!optimizationGoals.isEmpty()) {
            LOGGER.info("Schema optimizer generated {} new goal(s).", optimizationGoals.size());
            for (Thought goal : optimizationGoals) {
                handleNewThought(goal);
            }
        }
    }

    /**
     * Handles NewThoughtCreatedEvent from the event bus.
     * This is used for thoughts created outside the main cognition flow, e.g., by the Grounding system.
     * @param event The event containing the new thought.
     */
    public void onNewThoughtCreated(Events.NewThoughtCreatedEvent event) {
        LOGGER.debug("Received NewThoughtCreatedEvent for thought {}", event.thought().id());
        handleNewThought(event.thought());
    }

    private Optional<Thought> detectAndHandleCognitiveLoop(Thought currentFocus) {
        // Add current thought to history and maintain window size
        focusHistory.add(currentFocus.id());
        if (focusHistory.size() > FOCUS_HISTORY_WINDOW) {
            focusHistory.remove(0);
        }

        if (focusHistory.size() < FOCUS_HISTORY_WINDOW) {
            return Optional.empty(); // Not enough history to detect a loop
        }

        // Heuristic: Count unique thoughts in the history window
        long uniqueThoughts = focusHistory.stream().distinct().count();
        double repetitionRate = 1.0 - ((double) uniqueThoughts / FOCUS_HISTORY_WINDOW);

        if (repetitionRate > FOCUS_HISTORY_REPETITION_THRESHOLD) {
            LOGGER.warn("Cognitive loop/stall detected! Repetition rate: {}%. Intervening.", String.format("%.0f", repetitionRate * 100));

            // Create a meta-cognition goal to break the loop
            Thought metaGoal = createMetaCognitionGoal();
            handleNewThought(metaGoal); // Save and add to attention
            LOGGER.info("Overriding focus to meta-cognition goal {}", metaGoal.id());
            return Optional.of(metaGoal); // Override the current focus thought
        }

        return Optional.empty();
    }

    private Thought createMetaCognitionGoal() {
        String goalText = "A schema for analyzing the current cognitive state when stalled or in a loop and formulating a new plan.";
        ThoughtContent content = new ThoughtContent(goalText, null, null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                Collections.emptyList(), // This is a root-level intervention
                Instant.now()
        );
        // Extremely high salience to ensure it's the absolute next focus
        ThoughtState state = new ThoughtState(1.0, 999.0, 1.0);
        return new Thought(UUID.randomUUID().toString(), content, state, meta);
    }
}
