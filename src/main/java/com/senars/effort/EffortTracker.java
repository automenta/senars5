package com.senars.effort;

import com.senars.core.Thought;
import com.senars.events.Events;
import com.senars.events.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks the difference between predicted and actual effort for cognitive processes.
 * This class is thread-safe.
 */
public class EffortTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(EffortTracker.class);

    private final ConcurrentHashMap<String, TrackingInfo> inProgress = new ConcurrentHashMap<>();
    private final Queue<EffortRecord> effortRecords = new ConcurrentLinkedQueue<>();
    private final EffortPredictor effortPredictor;

    private static class TrackingInfo {
        final long startTime;
        final double predictedEffort;
        final int textLength;

        TrackingInfo(long startTime, double predictedEffort, int textLength) {
            this.startTime = startTime;
            this.predictedEffort = predictedEffort;
            this.textLength = textLength;
        }
    }

    public EffortTracker(EffortPredictor effortPredictor) {
        this.effortPredictor = effortPredictor;
    }

    public void onCognitionStart(Events.CognitionStartEvent event) {
        String thoughtId = event.thought().id();
        double predictedEffort = effortPredictor.predict(event.thought());
        int textLength = event.thought().content().text() != null ? event.thought().content().text().length() : 0;
        inProgress.put(thoughtId, new TrackingInfo(System.nanoTime(), predictedEffort, textLength));
    }

    public void onCognitionEnd(Events.CognitionEndEvent event) {
        String thoughtId = event.thought().id();
        TrackingInfo trackingInfo = inProgress.remove(thoughtId);
        if (trackingInfo != null) {
            long endTime = System.nanoTime();
            double actualEffort = (endTime - trackingInfo.startTime) / 1_000_000.0; // Convert to milliseconds

            EffortRecord record = new EffortRecord(
                    thoughtId,
                    trackingInfo.predictedEffort,
                    actualEffort,
                    trackingInfo.textLength
            );
            effortRecords.add(record);
            LOGGER.debug("Tracked effort for thought {}: Predicted={}, Actual={}", thoughtId, trackingInfo.predictedEffort, actualEffort);
        }
    }

    /**
     * Retrieves all recorded effort data and clears the internal store.
     * @return A list of all effort records captured since the last call.
     */
    public List<EffortRecord> drainRecords() {
        List<EffortRecord> records = new ArrayList<>();
        while (!effortRecords.isEmpty()) {
            records.add(effortRecords.poll());
        }
        return records;
    }
}
