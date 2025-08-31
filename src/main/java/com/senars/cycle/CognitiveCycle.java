package com.senars.cycle;

import com.senars.core.*;
import com.senars.systems.IGovernanceLayer;
import com.senars.systems.IMemoryNexus;

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

    private final IPerceptionSystem perceptionSystem;
    private final IAttentionFunnel attentionFunnel;
    private final ICognitiveProcessor cognitiveProcessor;
    private final IActionSystem actionSystem;
    private final IMemoryNexus memoryNexus;
    private final IGovernanceLayer governanceLayer;

    public CognitiveCycle(
        IPerceptionSystem perceptionSystem,
        IAttentionFunnel attentionFunnel,
        ICognitiveProcessor cognitiveProcessor,
        IActionSystem actionSystem,
        IMemoryNexus memoryNexus,
        IGovernanceLayer governanceLayer
    ) {
        this.perceptionSystem = Objects.requireNonNull(perceptionSystem);
        this.attentionFunnel = Objects.requireNonNull(attentionFunnel);
        this.cognitiveProcessor = Objects.requireNonNull(cognitiveProcessor);
        this.actionSystem = Objects.requireNonNull(actionSystem);
        this.memoryNexus = Objects.requireNonNull(memoryNexus);
        this.governanceLayer = Objects.requireNonNull(governanceLayer);
    }

    /**
     * Executes a single step of the cognitive cycle.
     */
    public void step() {
        // 1. Perception Stage
        List<Thought> perceivedThoughts = perceptionSystem.perceive();
        if (!perceivedThoughts.isEmpty()) {
            System.out.println("[CognitiveCycle] Perceived " + perceivedThoughts.size() + " new thoughts.");
            for (Thought thought : perceivedThoughts) {
                attentionFunnel.addCandidate(thought);
            }
        }

        // 2. Prioritization Stage
        Optional<Thought> focusThoughtOpt = attentionFunnel.selectFocusThought();

        // If there's nothing to focus on (even after perception), the cycle is idle.
        if (focusThoughtOpt.isEmpty()) {
            System.out.println("[CognitiveCycle] No focus thought. System is idle.");
            return;
        }

        Thought focusThought = focusThoughtOpt.get();
        System.out.println("[CognitiveCycle] Focusing on thought: " + focusThought.id());

        List<Thought> newThoughts = cognitiveProcessor.process(focusThought);

        for (Thought newThought : newThoughts) {
            handleNewThought(newThought);
        }
    }

    private void handleNewThought(Thought thought) {
        System.out.println("[CognitiveCycle] New thought generated: " + thought.metadata().type() + " - " + thought.id());
        memoryNexus.saveThought(thought);

        if (thought.metadata().type() == ThoughtType.ACTION_PLAN) {
            Optional<String> vetoReason = governanceLayer.reviewPlan(thought);
            if (vetoReason.isPresent()) {
                System.out.println("[CognitiveCycle] ACTION_PLAN vetoed: " + vetoReason.get());
                createReplanGoal(thought, vetoReason.get());
            } else {
                System.out.println("[CognitiveCycle] ACTION_PLAN approved. Executing...");
                actionSystem.executePlan(thought);
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
                null, null, null, null
            ),
            new ThoughtState(1.0, 100.0, 1.0), // High clarity, salience, and activation
            new ThoughtMetadata(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                List.of(vetoedPlan.id()),
                Instant.now()
            )
        );
        System.out.println("[CognitiveCycle] Created replan goal: " + replanGoal.id());
        memoryNexus.saveThought(replanGoal);
        attentionFunnel.addCandidate(replanGoal);
    }
}
