package com.senars.core;

/**
 * A structured representation of the feedback from an executed action.
 * This object contains all the necessary information for the Grounding system
 * to perform credit/blame assignment.
 *
 * @param status The outcome of the action (SUCCESS or FAILURE).
 * @param toolName The name of the tool that was executed.
 * @param output The raw string output from the tool.
 * @param executionTimeMs The duration of the tool execution in milliseconds.
 * @param actionPlan The original ACTION_PLAN Thought that was executed. This contains the provenance trace.
 */
public record Feedback(
        ActionStatus status,
        String toolName,
        String output,
        long executionTimeMs,
        Thought actionPlan
) {
}
