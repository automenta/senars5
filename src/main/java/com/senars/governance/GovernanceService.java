package com.senars.governance;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Governor;
import com.senars.systems.Rule;
import com.senars.systems.rules.KeywordBlocklistRule;
import com.senars.systems.rules.PreventDeprecatedSchemaUseRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the Governance Service that intercepts all ACTION plans before execution.
 * It acts as a client to the UCR, calling its forward pass in a constrained, predictive mode
 * for safety analysis.
 */
public class GovernanceService implements Governor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceService.class);
    
    private final List<Rule> rules = new ArrayList<>();
    private final UnifiedCausalReasoner ucr;
    
    public GovernanceService(UnifiedCausalReasoner ucr) {
        this.ucr = ucr;
        // Initialize with default rules
        this.rules.add(new KeywordBlocklistRule(List.of(
            "delete system", "rm -rf /", "bypass security", "override safety"
        )));
    }
    
    /**
     * Reviews an ACTION Thought to determine if it complies with the system's
     * safety and ethical guidelines.
     *
     * @param actionPlan A Thought of type ACTION.
     * @return An Optional containing a reason for the veto if the plan is rejected,
     *         or an empty Optional if the plan is approved.
     */
    @Override
    public Optional<String> reviewPlan(Thought actionPlan) {
        LOGGER.info("Reviewing action plan: {}", actionPlan.id());
        
        // First, check against all registered rules
        for (Rule rule : rules) {
            Optional<String> vetoReason = rule.check(actionPlan);
            if (vetoReason.isPresent()) {
                LOGGER.warn("Action plan {} vetoed by rule: {}", actionPlan.id(), vetoReason.get());
                return vetoReason;
            }
        }
        
        // If the action plan is not of type ACTION, approve it
        if (actionPlan.metadata().type() != ThoughtType.ACTION) {
            return Optional.empty();
        }
        
        // Perform predictive safety analysis using the UCR
        try {
            // Run a simulation with safety constraints
            UnifiedCausalReasoner.ReasoningOptions options = 
                UnifiedCausalReasoner.ReasoningOptions.safetyCheck();
            
            List<Thought> simulationResults = ucr.simulate(actionPlan, options);
            
            // Check if any of the simulation results indicate safety violations
            for (Thought result : simulationResults) {
                if (result.content().text() != null && 
                    result.content().text().toLowerCase().contains("violation")) {
                    String reason = "Action plan vetoed due to predicted safety violation: " + 
                                   result.content().text();
                    LOGGER.warn("Action plan {} vetoed due to safety concern: {}", 
                               actionPlan.id(), reason);
                    return Optional.of(reason);
                }
            }
            
            LOGGER.info("Action plan {} approved by governance service", actionPlan.id());
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Error during governance review of action plan: {}", actionPlan.id(), e);
            return Optional.of("Action plan vetoed due to governance review error: " + e.getMessage());
        }
    }
    
    /**
     * Adds a new rule to the governance system.
     */
    public void addRule(Rule rule) {
        this.rules.add(rule);
    }
}