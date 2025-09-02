package com.senars.cycle;

import com.senars.core.*;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.systems.Governor;
import com.senars.systems.Grounding;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;

/**
 * The core orchestrator of the SeNARS cognitive architecture.
 * It runs the main perception-attention-processing loop.
 */
public class CognitiveCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitiveCycle.class);
    private static final long OPTIMIZER_RUN_INTERVAL = 50;
    private static final int FOCUS_HISTORY_WINDOW = 10;
    private static final double FOCUS_HISTORY_REPETITION_THRESHOLD = 0.3; // If 30% of recent thoughts are the same, we might be looping
    private final Perception perception;
    private final Attention attention;
    private final Cognition cognition;
    private final Action action;
    private final Memory memory;
    private final Governor governor;
    private final Grounding grounding;
    private final ActionFeedbackQueue feedbackQueue;
    private final SchemaOptimizer schemaOptimizer;
    private final EffortModelOptimizer effortOptimizer;
    private final EffortTracker effortTracker;
    private final EventBus eventBus;
    private final List<String> focusHistory = new ArrayList<>();
    private long cycleCount = 0;


    public CognitiveCycle(
            Perception perception,
            Attention attention,
            Cognition cognition,
            Action action,
            Memory memory,
            Governor governor,
            Grounding grounding,
            ActionFeedbackQueue feedbackQueue,
            SchemaOptimizer schemaOptimizer,
            EffortModelOptimizer effortOptimizer,
            EffortTracker effortTracker,
            EventBus eventBus
    ) {
        this.perception = Objects.requireNonNull(perception);
        this.attention = Objects.requireNonNull(attention);
        this.cognition = Objects.requireNonNull(cognition);
        this.action = Objects.requireNonNull(action);
        this.memory = Objects.requireNonNull(memory);
        this.governor = Objects.requireNonNull(governor);
        this.grounding = Objects.requireNonNull(grounding);
        this.feedbackQueue = Objects.requireNonNull(feedbackQueue);
        this.schemaOptimizer = Objects.requireNonNull(schemaOptimizer);
        this.effortOptimizer = Objects.requireNonNull(effortOptimizer);
        this.effortTracker = Objects.requireNonNull(effortTracker);
        this.eventBus = Objects.requireNonNull(eventBus);
    }

    /**
     * Executes a single step of the cognitive cycle.
     * @throws ShutdownException if a shutdown is commanded through a perception channel.
     */
    public void step() throws ShutdownException {
        try {
            cycleCount++;
            runOptimizers();
            processActionFeedback();
            runPerception();

            Optional<Thought> focusThoughtOpt = attention.selectFocusThought();
            if (focusThoughtOpt.isEmpty()) {
                LOGGER.debug("No focus thought. System is idle.");
                return;
            }

            Thought focusThought = detectAndHandleCognitiveLoop(focusThoughtOpt.get())
                    .orElse(focusThoughtOpt.get());

            LOGGER.trace("Focusing on thought: {} - {}", focusThought.metadata().type(), focusThought.id());

            cognition.think(focusThought).forEach(this::handleNewThought);

        } catch (ShutdownException e) {
            throw e; // Propagate shutdown exception to the main loop
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during the cognitive cycle. Creating a meta-goal to analyze it.", e);
            handleSystemError(e);
        }
    }

    private void handleSystemError(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        String errorDetails = String.format("Error: %s\nStackTrace (first 500 chars):\n%s", e.getMessage(), stackTrace.substring(0, Math.min(500, stackTrace.length())));

        String goalText = "A critical system error occurred. Analyze the following error details and formulate a recovery plan. " + errorDetails;

        Thought errorGoal = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(goalText, "system_error_analysis", null, null, null, null, null),
                new ThoughtState(1.0, 999.0, 1.0), // Max salience to ensure it's handled next
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.SYSTEM,
                        Collections.emptyList(),
                        Instant.now()
                )
        );
        handleNewThought(errorGoal);
    }

    private void handleNewThought(Thought thought) {
        processThought(thought);
        eventBus.publish(new Events.NewThoughtCreatedEvent(thought));
    }

    /**
     * The core logic for processing a new thought, without publishing an event.
     * This method is called by the event handler to prevent recursion.
     * @param thought The thought to process.
     */
    private void processThought(Thought thought) {
        var type = thought.metadata().type();
        LOGGER.info("New thought generated: {} - {}", type, thought.id());
        memory.saveThought(thought);

        if (type == ThoughtType.ACTION) {
            handleActionPlan(thought);
        } else {
            // Check if this is a report generated from an explanation request
            if (type == ThoughtType.REPORT) {
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
                LOGGER.warn("ACTION vetoed: {}", reason);
                eventBus.publish(new Events.ActionPlanVetoedEvent(thought, reason));
                createReplanGoal(thought, reason);
            } else {
                LOGGER.info("ACTION approved. Executing...");
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
                .map(originatingThought -> originatingThought.metadata().type() == ThoughtType.EXPLAIN);
    }

    private void printExplanation(Thought report) {
        String border = "=======================================================================";
        String header = "🤖 S E N A R S :   E X P L A N A T I O N";
        System.out.println();
        System.out.println(border);
        System.out.println(header);
        System.out.println(border);
        System.out.println();
        // Simple word wrap for the explanation text
        String[] words = report.content().text().split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > border.length()) {
                System.out.println(line);
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        System.out.println(line); // Print the last line
        System.out.println();
        System.out.println(border);
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

    private void runPerception() throws ShutdownException {
        List<Thought> perceived = perception.perceive();
        if (!perceived.isEmpty()) {
            LOGGER.info("Perceived {} new thoughts from external sources.", perceived.size());
            perceived.forEach(this::handleNewThought);
        }
    }

    private void runOptimizers() {
        if (cycleCount % OPTIMIZER_RUN_INTERVAL == 0) {
            LOGGER.info("Cognitive cycle {} reached. Running optimizers.", cycleCount);

            // Run Schema Optimizer
            List<Thought> schemaGoals = schemaOptimizer.run();
            if (!schemaGoals.isEmpty()) {
                LOGGER.info("Schema optimizer generated {} new goal(s).", schemaGoals.size());
                schemaGoals.forEach(this::handleNewThought);
            }

            // Run Effort Model Optimizer
            List<Thought> effortGoals = effortOptimizer.run(effortTracker);
            if (!effortGoals.isEmpty()) {
                LOGGER.info("Effort model optimizer generated {} new goal(s).", effortGoals.size());
                effortGoals.forEach(this::handleNewThought);
            }
        }
    }

    /**
     * Handles NewThoughtCreatedEvent from the event bus.
     * This is used for thoughts created outside the main cognition flow, e.g., by the Grounding system.
     * This method calls processThought directly to avoid a recursive event loop.
     * @param event The event containing the new thought.
     */
    public void onNewThoughtCreated(Events.NewThoughtCreatedEvent event) {
        LOGGER.debug("Received NewThoughtCreatedEvent for thought {}", event.thought().id());
        // Call processThought directly to avoid re-publishing the event and causing an infinite loop.
        processThought(event.thought());
    }

    private Optional<Thought> detectAndHandleCognitiveLoop(Thought currentFocus) {
        // Add current thought to history and maintain window size
        focusHistory.add(currentFocus.id());
        if (focusHistory.size() > FOCUS_HISTORY_WINDOW) {
            focusHistory.removeFirst();
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
            Thought metaGoal = createMetaCognitionGoal(focusHistory);
            handleNewThought(metaGoal); // Save and add to attention
            LOGGER.info("Overriding focus to meta-cognition goal {}", metaGoal.id());
            return Optional.of(metaGoal); // Override the current focus thought
        }

        return Optional.empty();
    }

    private Thought createMetaCognitionGoal(List<String> focusHistory) {
        String history = String.join(", ", focusHistory);
        String goalText = String.format(
                "The system seems to be in a cognitive loop or stall. Analyze the recent focus history and formulate a new plan to break the loop. History: [%s]",
                history
        );

        ThoughtContent content = new ThoughtContent(goalText, "system_unstuck", null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                Collections.emptyList(), // This is a root-level intervention
                Instant.now()
        );

        return new Thought(UUID.randomUUID().toString(), content,
            // Extremely high salience to ensure it's the absolute next focus
            new ThoughtState(1.0, 999.0, 1.0),
        meta);
    }
}
