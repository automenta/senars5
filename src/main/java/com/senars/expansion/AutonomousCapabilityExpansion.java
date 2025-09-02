package com.senars.expansion;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.explanation.ExplanationService;
import com.senars.health.SystemHealthMonitor;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Memory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Implements Autonomous Capability Expansion, the final elegant combination of our services.
 * The System Health Monitor detects a class of unsolvable problems. It triggers the Meta-Cognition
 * Service, whose remediation plan is to generate a Thought containing a tool specification.
 * This Thought is then routed to the Explanation Service for presentation to a human developer.
 */
public class AutonomousCapabilityExpansion {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutonomousCapabilityExpansion.class);

    private final Memory memory;
    private final UnifiedCausalReasoner ucr;
    private final EventBus eventBus;
    private final ChatLanguageModel chatModel;
    private final ExplanationService explanationService;
    private final SystemHealthMonitor healthMonitor;

    public AutonomousCapabilityExpansion(
            Memory memory,
            UnifiedCausalReasoner ucr,
            EventBus eventBus,
            ChatLanguageModel chatModel,
            ExplanationService explanationService,
            SystemHealthMonitor healthMonitor) {
        this.memory = memory;
        this.ucr = ucr;
        this.eventBus = eventBus;
        this.chatModel = chatModel;
        this.explanationService = explanationService;
        this.healthMonitor = healthMonitor;
    }

    /**
     * Processes a systemic problem detected by the System Health Monitor.
     * If the problem is determined to be a class of unsolvable problems,
     * this method triggers the capability expansion process.
     *
     * @param problemReport The problem report from the System Health Monitor
     */
    public void processSystemicProblem(Thought problemReport) {
        try {
            LOGGER.info("Processing systemic problem: {}", problemReport.content().text());

            // Check if this is a class of unsolvable problems
            if (isUnsolvableProblemClass(problemReport)) {
                LOGGER.info("Detected unsolvable problem class. Initiating capability expansion.");

                // Trigger the Meta-Cognition Service to generate a remediation plan
                Thought remediationPlan = generateRemediationPlan(problemReport);

                // If the remediation plan involves creating a new tool, generate a tool specification
                if (requiresNewTool(remediationPlan)) {
                    Thought toolSpecification = generateToolSpecification(remediationPlan);

                    // Route the tool specification to the Explanation Service for presentation
                    presentToHumanDeveloper(toolSpecification);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing systemic problem", e);
        }
    }

    /**
     * Determines if a problem report represents a class of unsolvable problems.
     *
     * @param problemReport The problem report to analyze
     * @return true if it's an unsolvable problem class, false otherwise
     */
    private boolean isUnsolvableProblemClass(Thought problemReport) {
        String problemText = problemReport.content().text().toLowerCase();

        // Heuristics for identifying unsolvable problem classes
        return problemText.contains("repeated failure") ||
                problemText.contains("cannot solve") ||
                problemText.contains("capability gap") ||
                problemText.contains("persistent error") ||
                problemText.contains("systematic limitation");
    }

    /**
     * Generates a remediation plan for a systemic problem using the UCR.
     *
     * @param problemReport The problem report
     * @return A remediation plan
     */
    private Thought generateRemediationPlan(Thought problemReport) {
        try {
            // Create a goal to generate a remediation plan
            String goalText = String.format(
                    "Generate a remediation plan for the systemic problem: %s",
                    problemReport.content().text()
            );

            ThoughtContent content = new ThoughtContent(
                    goalText,
                    "expansion:remediation_plan",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            ThoughtMeta meta = new ThoughtMeta(
                    ThoughtType.GOAL,
                    ThoughtOrigin.SYSTEM,
                    Collections.singletonList(problemReport.id()),
                    Instant.now()
            );

            ThoughtState state = new ThoughtState(1.0, 900.0, 1.0); // High salience

            Thought remediationGoal = new Thought(
                    UUID.randomUUID().toString(),
                    content,
                    state,
                    meta
            );

            // Use the UCR to generate the remediation plan
            List<Thought> planResults = ucr.reason(
                    remediationGoal,
                    "forward",
                    UnifiedCausalReasoner.ReasoningOptions.defaults()
            );

            // Find the action plan in the results
            return planResults.stream()
                    .filter(thought -> thought.metadata().type() == ThoughtType.ACTION)
                    .findFirst()
                    .orElse(remediationGoal); // Fallback to the goal itself
        } catch (Exception e) {
            LOGGER.error("Error generating remediation plan", e);

            // Create a fallback remediation plan
            return new Thought(
                    UUID.randomUUID().toString(),
                    new ThoughtContent(
                            "Investigate the systemic problem and determine necessary capabilities",
                            "expansion:fallback_plan",
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new ThoughtState(1.0, 900.0, 1.0),
                    new ThoughtMeta(
                            ThoughtType.GOAL,
                            ThoughtOrigin.SYSTEM,
                            Collections.singletonList(problemReport.id()),
                            Instant.now()
                    )
            );
        }
    }

    /**
     * Determines if a remediation plan requires a new tool.
     *
     * @param remediationPlan The remediation plan to analyze
     * @return true if a new tool is required, false otherwise
     */
    private boolean requiresNewTool(Thought remediationPlan) {
        String planText = remediationPlan.content().text().toLowerCase();

        // Heuristics for identifying when a new tool is needed
        return planText.contains("new tool") ||
                planText.contains("missing capability") ||
                planText.contains("external integration") ||
                planText.contains("api access") ||
                planText.contains("specialized function");
    }

    /**
     * Generates a tool specification based on a remediation plan.
     *
     * @param remediationPlan The remediation plan
     * @return A tool specification thought
     */
    private Thought generateToolSpecification(Thought remediationPlan) {
        try {
            String prompt = String.format(
                    """
                            You are an AI system designer. Based on the following remediation plan, \
                            generate a detailed tool specification that would address the capability gap.
                            
                            Remediation plan:
                            %s
                            
                            Please provide a JSON specification for a new tool that would solve this problem. \
                            Include the following fields:
                            - name: A concise name for the tool
                            - description: A detailed description of what the tool does
                            - parameters: A list of parameters the tool accepts
                            - implementation_notes: Notes on how to implement this tool
                            
                            Respond ONLY with the JSON specification, nothing else.""",
                    remediationPlan.content().text()
            );

            String toolSpecText = chatModel.generate(UserMessage.from(prompt)).content().text();

            // Create a tool specification thought
            return new Thought(
                    UUID.randomUUID().toString(),
                    new ThoughtContent(
                            toolSpecText,
                            "expansion:tool_specification",
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new ThoughtState(1.0, 950.0, 1.0), // Very high salience
                    new ThoughtMeta(
                            ThoughtType.REPORT,
                            ThoughtOrigin.SYSTEM,
                            Collections.singletonList(remediationPlan.id()),
                            Instant.now()
                    )
            );
        } catch (Exception e) {
            LOGGER.error("Error generating tool specification", e);

            // Create a fallback tool specification
            return new Thought(
                    UUID.randomUUID().toString(),
                    new ThoughtContent(
                            "{\n  \"name\": \"UndefinedTool\",\n  \"description\": \"A tool to address the capability gap identified in the remediation plan\",\n  \"parameters\": [],\n  \"implementation_notes\": \"Implement based on the remediation plan: " + remediationPlan.content().text() + "\"\n}",
                            "expansion:tool_specification",
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new ThoughtState(1.0, 950.0, 1.0),
                    new ThoughtMeta(
                            ThoughtType.REPORT,
                            ThoughtOrigin.SYSTEM,
                            Collections.singletonList(remediationPlan.id()),
                            Instant.now()
                    )
            );
        }
    }

    /**
     * Presents a tool specification to a human developer via the Explanation Service.
     *
     * @param toolSpecification The tool specification to present
     */
    private void presentToHumanDeveloper(Thought toolSpecification) {
        try {
            LOGGER.info("Presenting tool specification to human developer");

            // In a real implementation, this would interface with a UI or notification system
            // For now, we'll just log it and create a presentation thought

            // Create a presentation thought that could be picked up by a UI component
            Thought presentationThought = new Thought(
                    UUID.randomUUID().toString(),
                    new ThoughtContent(
                            "New Tool Specification Required\n\n" + toolSpecification.content().text(),
                            "expansion:presentation",
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    new ThoughtState(1.0, 1000.0, 1.0), // Maximum salience
                    new ThoughtMeta(
                            ThoughtType.REPORT,
                            ThoughtOrigin.SYSTEM,
                            Collections.singletonList(toolSpecification.id()),
                            Instant.now()
                    )
            );

            // Publish the presentation thought
            eventBus.publish(new Events.NewThoughtCreatedEvent(presentationThought));

            LOGGER.info("Tool specification presented to human developer: {}", toolSpecification.content().text());
        } catch (Exception e) {
            LOGGER.error("Error presenting tool specification to human developer", e);
        }
    }
}