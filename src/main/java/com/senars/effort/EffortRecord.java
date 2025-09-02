package com.senars.effort;

import com.senars.core.Thought;

/**
 * A record to store the data for a single thought processing event.
 *
 * @param thoughtId The ID of the thought that was processed.
 * @param predictedEffort The effort that was predicted for processing the thought.
 * @param actualEffort The actual effort (e.g., in milliseconds) it took to process the thought.
 * @param textLength The length of the text content of the thought.
 */
public record EffortRecord(
        String thoughtId,
        double predictedEffort,
        double actualEffort,
        int textLength
) {
}
