package com.senars.cycle;

import com.senars.effort.EffortTracker;
import com.senars.events.EventBus;
import com.senars.logic.GoalOrientedPlanner;
import com.senars.logic.MetaCognitiveService;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.logic.mdr.MDRService;
import com.senars.optimizer.EffortModelOptimizer;
import com.senars.optimizer.SchemaOptimizer;
import com.senars.systems.Governor;
import com.senars.systems.Memory;
import com.senars.ui.ConsolePrinter;

public record CognitiveCycleServices(
        Perception perception,
        Attention attention,
        UnifiedCausalReasoner ucr,
        Action action,
        Memory memory,
        Governor governor,
        ActionFeedbackQueue feedbackQueue,
        SchemaOptimizer schemaOptimizer,
        EffortModelOptimizer effortOptimizer,
        EffortTracker effortTracker,
        EventBus eventBus,
        MetaCognitiveService metaCognitiveService,
        MDRService mdrService,
        GoalOrientedPlanner goalOrientedPlanner,
        ConsolePrinter consolePrinter
) {
}
