package com.senars.cycle;

import com.senars.attention.Attention;
import com.senars.core.*;
import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.logic.GoalOrientedPlanner;
import com.senars.logic.MetaCognitiveService;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.logic.mdr.MDRService;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.systems.Governor;
import com.senars.systems.Memory;
import com.senars.ui.ConsolePrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final UnifiedCausalReasoner ucr; // The UCR handles both forward and backward reasoning
    private final Action action;
    private final Memory memory;
    private final Governor governor;
    private final ActionFeedbackQueue feedbackQueue;
    private final SchemaOptimizer schemaOptimizer;
    private final EffortModelOptimizer effortOptimizer;
    private final EffortTracker effortTracker;
    private final EventBus eventBus;
    private final MetaCognitiveService metaCognitiveService;
    private final MDRService mdrService;
    private final GoalOrientedPlanner goalOrientedPlanner;
    private final ConsolePrinter consolePrinter;
    private final List<Thought> focusHistory = new ArrayList<>();
    private long cycleCount = 0;


    public CognitiveCycle(CognitiveCycleServices services) {
        this.perception = Objects.requireNonNull(services.perception());
        this.attention = Objects.requireNonNull(services.attention());
        this.ucr = Objects.requireNonNull(services.ucr());
        this.action = Objects.requireNonNull(services.action());
        this.memory = Objects.requireNonNull(services.memory());
        this.governor = Objects.requireNonNull(services.governor());
        this.feedbackQueue = Objects.requireNonNull(services.feedbackQueue());
        this.schemaOptimizer = Objects.requireNonNull(services.schemaOptimizer());
        this.effortOptimizer = Objects.requireNonNull(services.effortOptimizer());
        this.effortTracker = Objects.requireNonNull(services.effortTracker());
        this.eventBus = Objects.requireNonNull(services.eventBus());
        this.metaCognitiveService = Objects.requireNonNull(services.metaCognitiveService());
        this.mdrService = Objects.requireNonNull(services.mdrService());
        this.goalOrientedPlanner = Objects.requireNonNull(services.goalOrientedPlanner());
        this.consolePrinter = Objects.requireNonNull(services.consolePrinter());
    }

    /**
     * Executes a single step of the cognitive cycle.
     * @throws ShutdownException if a shutdown is commanded through a perception channel.
     */
    public void step() throws ShutdownException {
        try {
            cycleCount++;
            runPreCycleTasks();

            attention.selectFocusThought()
                    .ifPresentOrElse(
                            this::processFocusThought,
                            this::handleIdleState
                    );
        } catch (ShutdownException e) {
            throw e; // Propagate shutdown exception to the main loop
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during the cognitive cycle. Creating a meta-goal to analyze it.", e);
            handleSystemError(e);
        }
    }

    private void runPreCycleTasks() throws ShutdownException {
        runOptimizers();
        processActionFeedback();
        runPerception();
    }

    private void processFocusThought(Thought thought) {
        Thought focusThought = detectAndHandleCognitiveLoop(thought)
                .orElse(thought);

        LOGGER.trace("Focusing on thought: {} - {}", focusThought.metadata().type(), focusThought.id());

        // Use the UCR for forward reasoning instead of the Cognitive Processor
        ucr.reason(focusThought, "forward", UnifiedCausalReasoner.ReasoningOptions.defaults())
                .forEach(this::handleNewThought);
    }

    private void handleIdleState() {
        LOGGER.debug("No focus thought. System is idle. Engaging Goal Oriented Planner.");
        goalOrientedPlanner.generateNextTask().ifPresent(this::handleNewThought);
    }

    private void handleSystemError(Exception e) {
        // Delegate error analysis to the MetaCognitiveService
        Thought recoveryGoal = metaCognitiveService.analyzeSystemError(e).join();
        if (recoveryGoal != null) {
            handleNewThought(recoveryGoal);
        }
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
                isCausalExplanationReport(thought).ifPresent(isExplanation -> {
                    if (isExplanation) {
                        consolePrinter.printExplanation(thought);
                    }
                });
            }
            attention.addCandidate(thought);
        }
    }

    private void handleActionPlan(Thought thought) {
        try {
            // First, perform predictive grounding simulation to identify potential issues
            List<Thought> simulations = ucr.simulate(thought, UnifiedCausalReasoner.ReasoningOptions.simulation());

            // Check if any simulations indicate potential problems
            boolean hasPotentialIssues = simulations.stream()
                    .anyMatch(sim -> sim.state().clarity() < 0.7); // Threshold for potential issues

            if (hasPotentialIssues) {
                LOGGER.warn("Predictive grounding simulation detected potential issues with action plan: {}", thought.id());
                // Create a replan goal to address the potential issues
                StringBuilder issuesText = new StringBuilder("due to potential issues detected in simulation: ");
                for (Thought simulation : simulations) {
                    if (simulation.state().clarity() < 0.7) {
                        issuesText.append(simulation.content().text()).append(" ");
                    }
                }
                createReplanGoal(thought, issuesText.toString().trim());
                return;
            }

            // If no issues detected, proceed with governance review
            Optional<String> vetoReason = governor.reviewPlan(thought);
            if (vetoReason.isPresent()) {
                String reason = vetoReason.get();
                LOGGER.warn("ACTION vetoed: {}", reason);
                eventBus.publish(new Events.ActionPlanVetoedEvent(thought, reason));
                createReplanGoal(thought, "due to safety violation: " + reason);
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
            // First, let the UCR process the feedback for credit/blame assignment
            ucr.processFeedback(feedback);

            // Then, let the MDR service check if any self-correction is needed
            mdrService.processFeedback(feedback).ifPresent(this::handleNewThought);
        }
    }

    private Optional<Boolean> isCausalExplanationReport(Thought report) {
        List<String> trace = report.metadata().trace();
        if (trace == null || trace.isEmpty()) {
            return Optional.of(false);
        }
        // Check if the report traces back to an explanation request
        return memory.getThoughtById(trace.getFirst())
                .map(originatingThought -> originatingThought.metadata().type() == ThoughtType.EXPLAIN);
    }

    private void createReplanGoal(Thought problematicPlan, String reason) {
        String newId = UUID.randomUUID().toString();
        Thought replanGoal = new Thought(
                newId,
                new ThoughtContent(
                        "Reformulate plan " + problematicPlan.id() + " " + reason,
                        null, null, null, null, null, null
                ),
                new ThoughtState(1.0, 100.0, 1.0), // High clarity, salience, and activation
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.SYSTEM,
                        List.of(problematicPlan.id()),
                        Instant.now()
                )
        );
        LOGGER.info("Created replan goal for plan {}: {}", problematicPlan.id(), reason);
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
        focusHistory.add(currentFocus);
        if (focusHistory.size() > FOCUS_HISTORY_WINDOW) {
            focusHistory.removeFirst();
        }

        if (focusHistory.size() < FOCUS_HISTORY_WINDOW) {
            return Optional.empty(); // Not enough history to detect a loop
        }

        // Heuristic: Count unique thoughts in the history window
        long uniqueThoughtIds = focusHistory.stream().map(Thought::id).distinct().count();
        double repetitionRate = 1.0 - ((double) uniqueThoughtIds / FOCUS_HISTORY_WINDOW);

        if (repetitionRate > FOCUS_HISTORY_REPETITION_THRESHOLD) {
            LOGGER.warn("Cognitive loop/stall detected! Repetition rate: {}%. Intervening.", String.format("%.0f", repetitionRate * 100));

            // Delegate loop analysis to the MetaCognitiveService
            Thought metaGoal = metaCognitiveService.analyzeCognitiveStall(new ArrayList<>(focusHistory)).join();

            if (metaGoal != null) {
                handleNewThought(metaGoal); // Save and add to attention
                LOGGER.info("Overriding focus to meta-cognition goal {}", metaGoal.id());
                return Optional.of(metaGoal); // Override the current focus thought
            }
        }

        return Optional.empty();
    }
}
