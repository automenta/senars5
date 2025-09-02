package com.senars.logic.mdr;

import com.senars.core.*;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Memory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implements the Monitor-Diagnose-Remediate pattern as a generic service.
 * This service cleanly separates the policy of self-correction from the mechanism of reasoning.
 */
public class MDRService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MDRService.class);

    private final UnifiedCausalReasoner ucr;
    private final Memory memory;
    private final ChatLanguageModel chatModel;
    private final List<MonitorConfig> monitors;

    public MDRService(UnifiedCausalReasoner ucr, Memory memory, ChatLanguageModel chatModel) {
        this.ucr = Objects.requireNonNull(ucr);
        this.memory = Objects.requireNonNull(memory);
        this.chatModel = Objects.requireNonNull(chatModel);
        this.monitors = new ArrayList<>();
        
        // Register default monitors
        registerDefaultMonitors();
    }

    /**
     * Registers the default monitors for the MDR service.
     */
    private void registerDefaultMonitors() {
        // Monitor for failed actions
        MonitorConfig failedActionMonitor = new MonitorConfig(
                "FailedActionMonitor",
                feedback -> feedback.status() == ActionStatus.FAILURE,
                "Monitors for failed action executions and triggers diagnostic analysis"
        );
        monitors.add(failedActionMonitor);
        
        LOGGER.info("Registered {} default monitors", monitors.size());
    }

    /**
     * Adds a new monitor configuration to the service.
     * 
     * @param monitorConfig The monitor configuration to add
     */
    public void addMonitor(MonitorConfig monitorConfig) {
        monitors.add(monitorConfig);
        LOGGER.info("Added new monitor: {}", monitorConfig.getName());
    }

    /**
     * Processes feedback from executed actions to detect issues and trigger self-correction.
     * This is the "Monitor" part of the MDR pattern.
     * 
     * @param feedback The feedback from an executed action
     * @return Optional containing a remediation goal if an issue was detected and diagnosed
     */
    public Optional<Thought> processFeedback(Feedback feedback) {
        LOGGER.debug("Processing feedback for tool '{}'", feedback.toolName());
        
        // Check if any monitor is triggered by this feedback
        Optional<MonitorConfig> triggeredMonitor = monitors.stream()
                .filter(monitor -> monitor.getTriggerCondition().test(feedback))
                .findFirst();
                
        if (triggeredMonitor.isPresent()) {
            LOGGER.info("Monitor '{}' triggered for feedback from tool '{}'", 
                    triggeredMonitor.get().getName(), feedback.toolName());
            
            // Perform diagnosis and remediation
            return diagnoseAndRemediate(feedback, triggeredMonitor.get());
        }
        
        return Optional.empty();
    }

    /**
     * Performs the diagnosis and remediation process for a triggered monitor.
     * This is the "Diagnose" and "Remediate" parts of the MDR pattern.
     * 
     * @param feedback The feedback that triggered the monitor
     * @param monitor The monitor that was triggered
     * @return Optional containing a remediation goal
     */
    private Optional<Thought> diagnoseAndRemediate(Feedback feedback, MonitorConfig monitor) {
        try {
            LOGGER.info("Starting diagnosis for failed action using UCR backward reasoning");
            
            // Use the UCR's backward reasoning to diagnose the root cause
            List<Thought> diagnosisResults = ucr.reason(
                    feedback.actionPlan(), 
                    "backward", 
                    UnifiedCausalReasoner.ReasoningOptions.defaults()
            );
            
            if (diagnosisResults.isEmpty()) {
                LOGGER.warn("UCR backward reasoning produced no results for diagnosis");
                return Optional.empty();
            }
            
            // Find the diagnostic report (should be a REPORT thought)
            Optional<Thought> diagnosticReport = diagnosisResults.stream()
                    .filter(thought -> thought.metadata().type() == ThoughtType.REPORT)
                    .findFirst();
                    
            if (diagnosticReport.isEmpty()) {
                LOGGER.warn("No diagnostic report found in UCR results");
                return Optional.empty();
            }
            
            LOGGER.info("Diagnosis complete. Generating remediation plan.");
            
            // Generate a remediation goal based on the diagnosis
            Thought remediationGoal = generateRemediationGoal(feedback, diagnosticReport.get(), monitor);
            
            return Optional.of(remediationGoal);
            
        } catch (Exception e) {
            LOGGER.error("Error during diagnosis and remediation process", e);
            return Optional.empty();
        }
    }

    /**
     * Generates a remediation goal based on the diagnosis report.
     * 
     * @param feedback The original feedback
     * @param diagnosisReport The diagnosis report from the UCR
     * @param monitor The monitor that triggered the process
     * @return A remediation goal thought
     */
    private Thought generateRemediationGoal(Feedback feedback, Thought diagnosisReport, MonitorConfig monitor) {
        String goalText;
        
        // Special handling for schema optimization
        if (monitor instanceof SchemaOptimizationMonitorConfig) {
            SchemaOptimizationMonitorConfig schemaMonitor = (SchemaOptimizationMonitorConfig) monitor;
            if (schemaMonitor.isSchemaRelatedFailure(feedback)) {
                goalText = String.format(
                        "Optimize the schema related to the failure in tool '%s'. Diagnosis: %s",
                        feedback.toolName(),
                        diagnosisReport.content().text()
                );
            } else {
                goalText = String.format(
                        "Fix the root cause of the failure in tool '%s'. Diagnosis: %s",
                        feedback.toolName(),
                        diagnosisReport.content().text()
                );
            }
        } else {
            goalText = String.format(
                    "Fix the root cause of the failure in tool '%s'. Diagnosis: %s",
                    feedback.toolName(),
                    diagnosisReport.content().text()
            );
        }
        
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent(goalText, "mdr:remediation", null, null, null, null, null),
                new ThoughtState(1.0, 900.0, 1.0), // High salience to ensure it's prioritized
                new ThoughtMeta(
                        ThoughtType.GOAL,
                        ThoughtOrigin.META_COGNITION,
                        List.of(feedback.actionPlan().id(), diagnosisReport.id()),
                        Instant.now()
                )
        );
    }
}