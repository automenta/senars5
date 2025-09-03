package com.senars.io;

import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.cycle.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the IActionSystem that interacts with the user via the system console.
 * It prints the action plan to the console and prompts the user for feedback.
 */
public class ConsoleAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleAction.class);

    @Override
    public Feedback executePlan(Thought actionPlan) {
        long startTime = System.currentTimeMillis();

        if (actionPlan == null || actionPlan.metadata().type() != ThoughtType.ACTION) {
            LOGGER.warn("Attempted to execute a thought that was not an ACTION. Thought ID: {}", actionPlan != null ? actionPlan.id() : "null");
            return new Feedback(ActionStatus.FAILURE, "internal", "Invalid action plan thought", 0, actionPlan);
        }

        String planText = actionPlan.content().text();

        System.out.println(); // Add a blank line for readability
        System.out.println("========================================");
        System.out.println("🤖 CONSOLE ACTION");
        System.out.println("----------------------------------------");
        if (planText == null || planText.isBlank()) {
            System.out.println("(Action plan has no textual content)");
            LOGGER.warn("Executed an ACTION with no text content. Thought ID: {}", actionPlan.id());
        } else {
            System.out.println(planText);
        }
        System.out.println("========================================");
        System.out.println();

        // In this simple console action, we assume success and provide a default observation.
        // A more sophisticated implementation would interact with the user to get feedback.
        String observation = "Console action executed. User has been notified.";
        long executionTime = System.currentTimeMillis() - startTime;
        return new Feedback(ActionStatus.SUCCESS, "console.notify", observation, executionTime, actionPlan);
    }
}
