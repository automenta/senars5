package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.cycle.IActionSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the IActionSystem that interacts with the user via the system console.
 * It prints the action plan to the console and prompts the user for feedback.
 */
public class ConsoleActionSystem implements IActionSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleActionSystem.class);

    @Override
    public void executePlan(Thought actionPlan) {
        if (actionPlan == null || actionPlan.metadata().type() != ThoughtType.ACTION_PLAN) {
            LOGGER.warn("Attempted to execute a thought that was not an ACTION_PLAN. Thought ID: {}", actionPlan != null ? actionPlan.id() : "null");
            return;
        }

        String planText = actionPlan.content().text();

        System.out.println(); // Add a blank line for readability
        System.out.println("========================================");
        System.out.println("🤖 EXECUTING ACTION PLAN");
        System.out.println("----------------------------------------");
        if (planText == null || planText.isBlank()) {
            System.out.println("(Action plan has no textual content)");
            LOGGER.warn("Executed an ACTION_PLAN with no text content. Thought ID: {}", actionPlan.id());
        } else {
            System.out.println(planText);
        }
        System.out.println("========================================");
        System.out.println();

        // Prompt the user for feedback. The perception system will be responsible for reading this input in a subsequent cycle.
        System.out.print("> Please provide feedback for this action (e.g., 'feedback: 0.9' for success, 'feedback: 0.2' for failure): ");
    }
}
