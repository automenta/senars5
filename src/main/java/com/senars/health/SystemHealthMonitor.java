package com.senars.health;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The System Health Monitor performs complex analytical queries on the Memory graph
 * to detect systemic trends and issues. When it detects a problem, it creates a "problem"
 * Thought and feeds it to the MDR service.
 */
public class SystemHealthMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemHealthMonitor.class);
    // Thresholds for health metrics
    private static final double REASONING_TIME_THRESHOLD = 1000.0; // ms
    private static final double FAILURE_RATE_THRESHOLD = 0.3; // 30%
    private static final int MEMORY_GROWTH_THRESHOLD = 1000; // thoughts per hour
    private final Memory memory;
    private final UnifiedCausalReasoner ucr;
    private final EventBus eventBus;
    private final ScheduledExecutorService scheduler;

    public SystemHealthMonitor(Memory memory, UnifiedCausalReasoner ucr, EventBus eventBus) {
        this.memory = memory;
        this.ucr = ucr;
        this.eventBus = eventBus;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Starts the health monitoring process.
     */
    public void startMonitoring() {
        LOGGER.info("Starting system health monitoring");

        // Schedule periodic health checks
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Stops the health monitoring process.
     */
    public void stopMonitoring() {
        LOGGER.info("Stopping system health monitoring");
        scheduler.shutdown();
    }

    /**
     * Performs a health check on the system by analyzing various metrics.
     */
    private void performHealthCheck() {
        try {
            LOGGER.debug("Performing system health check");

            // Check for increasing reasoning time
            checkReasoningTimeTrend();

            // Check for high failure rate
            checkFailureRate();

            // Check for memory growth issues
            checkMemoryGrowth();

        } catch (Exception e) {
            LOGGER.error("Error during health check", e);
        }
    }

    /**
     * Checks if reasoning time is increasing over time, which could indicate
     * performance degradation or inefficient memory usage.
     */
    private void checkReasoningTimeTrend() {
        try {
            // Get recent reasoning time data
            List<Double> recentReasoningTimes = getRecentReasoningTimes(100);

            if (recentReasoningTimes.size() < 10) {
                return; // Not enough data
            }

            // Calculate trend using simple linear regression
            double trend = calculateTrend(recentReasoningTimes);

            if (trend > REASONING_TIME_THRESHOLD) {
                LOGGER.warn("Detected increasing reasoning time trend: {}", trend);

                // Create a problem thought
                Thought problemThought = createProblemThought(
                        "Increasing Reasoning Time",
                        String.format("Reasoning time is increasing at a rate of %.2f ms per operation. This could indicate performance degradation or memory issues.", trend),
                        "reasoning_time_trend"
                );

                // Publish the problem thought
                eventBus.publish(new Events.NewThoughtCreatedEvent(problemThought));
            }
        } catch (Exception e) {
            LOGGER.error("Error checking reasoning time trend", e);
        }
    }

    /**
     * Checks if the failure rate is too high, which could indicate systemic issues.
     */
    private void checkFailureRate() {
        try {
            // Get recent action results
            List<Feedback> recentFeedback = getRecentFeedback(100);

            if (recentFeedback.size() < 10) {
                return; // Not enough data
            }

            // Calculate failure rate
            long failureCount = recentFeedback.stream()
                    .map(Feedback::status)
                    .filter(status -> status == ActionStatus.FAILURE)
                    .count();

            double failureRate = (double) failureCount / recentFeedback.size();

            if (failureRate > FAILURE_RATE_THRESHOLD) {
                LOGGER.warn("Detected high failure rate: {}%", failureRate * 100);

                // Create a problem thought
                Thought problemThought = createProblemThought(
                        "High Failure Rate",
                        String.format("Failure rate is %.1f%%, which exceeds the threshold of %.0f%%. This indicates systemic issues that need attention.",
                                failureRate * 100, FAILURE_RATE_THRESHOLD * 100),
                        "high_failure_rate"
                );

                // Publish the problem thought
                eventBus.publish(new Events.NewThoughtCreatedEvent(problemThought));
            }
        } catch (Exception e) {
            LOGGER.error("Error checking failure rate", e);
        }
    }

    /**
     * Checks if memory is growing too rapidly, which could indicate memory leaks
     * or inefficient memory management.
     */
    private void checkMemoryGrowth() {
        try {
            // Get memory size at different time points
            long currentSize = memory.getAllThoughts().size();
            long sizeOneHourAgo = getMemorySizeAtTime(System.currentTimeMillis() - 3600000); // 1 hour ago

            long growth = currentSize - sizeOneHourAgo;

            if (growth > MEMORY_GROWTH_THRESHOLD) {
                LOGGER.warn("Detected rapid memory growth: {} thoughts in the last hour", growth);

                // Create a problem thought
                Thought problemThought = createProblemThought(
                        "Rapid Memory Growth",
                        String.format("Memory has grown by %d thoughts in the last hour, which exceeds the threshold of %d. This could indicate memory leaks or inefficient management.",
                                growth, MEMORY_GROWTH_THRESHOLD),
                        "memory_growth"
                );

                // Publish the problem thought
                eventBus.publish(new Events.NewThoughtCreatedEvent(problemThought));
            }
        } catch (Exception e) {
            LOGGER.error("Error checking memory growth", e);
        }
    }

    /**
     * Gets recent reasoning times from the memory.
     *
     * @param limit The maximum number of times to retrieve
     * @return A list of recent reasoning times
     */
    private List<Double> getRecentReasoningTimes(int limit) {
        // In a real implementation, this would retrieve actual reasoning time data
        // For now, we'll return an empty list as a placeholder
        return new ArrayList<>();
    }

    /**
     * Gets recent feedback from the memory.
     *
     * @param limit The maximum number of feedback items to retrieve
     * @return A list of recent feedback items
     */
    private List<Feedback> getRecentFeedback(int limit) {
        // In a real implementation, this would retrieve actual feedback data
        // For now, we'll return an empty list as a placeholder
        return new ArrayList<>();
    }

    /**
     * Gets the memory size at a specific time.
     *
     * @param timestamp The timestamp to check
     * @return The memory size at that time
     */
    private long getMemorySizeAtTime(long timestamp) {
        // In a real implementation, this would retrieve memory size at a specific time
        // For now, we'll return 0 as a placeholder
        return 0;
    }

    /**
     * Calculates the trend of a series of values using simple linear regression.
     *
     * @param values The values to analyze
     * @return The trend (slope of the regression line)
     */
    private double calculateTrend(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        int n = values.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXX = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        // Calculate slope (trend)
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);

        return slope;
    }

    /**
     * Creates a problem thought to be fed to the MDR service.
     *
     * @param title The title of the problem
     * @param description The detailed description of the problem
     * @param category The category of the problem
     * @return A problem thought
     */
    private Thought createProblemThought(String title, String description, String category) {
        String text = String.format("[%s] %s: %s", category, title, description);

        ThoughtContent content = new ThoughtContent(
                text,
                "health_monitor:problem_report",
                null,
                null,
                null,
                null,
                null
        );

        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.REPORT,
                ThoughtOrigin.SYSTEM,
                Collections.emptyList(),
                Instant.now()
        );

        // High salience to ensure it's prioritized
        ThoughtState state = new ThoughtState(1.0, 950.0, 1.0);

        return new Thought(
                UUID.randomUUID().toString(),
                content,
                state,
                meta
        );
    }
}