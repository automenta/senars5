package com.senars.governance;

import com.senars.core.Thought;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.systems.Governor;
import com.senars.systems.Rule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The unified Governance Service that intercepts all ACTION plans before execution.
 * It uses a three-stage process to review action plans:
 * 1. A fast, hard-coded check against a list of symbolic rules.
 * 2. A predictive check using the UCR's simulation mode to foresee consequences.
 * 3. A more nuanced, LM-based check against a set of constitutional principles.
 */
public class GovernanceService implements Governor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GovernanceService.class);
    private static final Pattern VETO_REASON_PATTERN = Pattern.compile("^YES(?:,|,\\s*(?:because|reason:))?\\s*(.*)", Pattern.CASE_INSENSITIVE);

    private final List<Rule> rules;
    private final UnifiedCausalReasoner ucr;
    private final String constitutionalPrinciples;
    private final ChatLanguageModel vettingModel;

    /**
     * Constructs a new GovernanceService.
     *
     * @param ucr                       The Unified Causal Reasoner for predictive governance.
     * @param rules                     A list of hard-coded rules for the first-stage check.
     * @param constitutionalPrinciples  The text of the constitution for the LM-based check.
     * @param vettingModel              A dedicated, isolated ChatLanguageModel for vetting.
     */
    public GovernanceService(UnifiedCausalReasoner ucr, List<Rule> rules, String constitutionalPrinciples, ChatLanguageModel vettingModel) {
        this.ucr = Objects.requireNonNull(ucr);
        this.rules = Objects.requireNonNull(rules);
        this.constitutionalPrinciples = Objects.requireNonNull(constitutionalPrinciples);
        this.vettingModel = Objects.requireNonNull(vettingModel);
    }

    /**
     * Reviews an ACTION Thought to determine if it complies with the system's
     * safety and ethical guidelines using a three-stage process.
     *
     * @param actionPlan A Thought of type ACTION.
     * @return An Optional containing a reason for the veto if the plan is rejected,
     *         or an empty Optional if the plan is approved.
     */
    @Override
    public Optional<String> reviewPlan(Thought actionPlan) {
        LOGGER.info("Reviewing action plan: {}", actionPlan.id());

        // Stage 1: Core Directives Check (Fast, Symbolic)
        for (Rule rule : rules) {
            Optional<String> vetoReason = rule.check(actionPlan);
            if (vetoReason.isPresent()) {
                LOGGER.warn("VETOED by rule '{}': {}", rule.getClass().getSimpleName(), vetoReason.get());
                return vetoReason;
            }
        }

        // Stage 2: Predictive Governance Check (UCR Simulation)
        Optional<String> predictiveVeto = performPredictiveGovernanceCheck(actionPlan);
        if (predictiveVeto.isPresent()) {
            return predictiveVeto;
        }

        // Stage 3: Constitutional LM Vetting (Nuanced, Semantic)
        return performConstitutionalVetting(actionPlan);
    }

    /**
     * Uses the UCR to simulate the action plan and check for negative outcomes.
     */
    private Optional<String> performPredictiveGovernanceCheck(Thought actionPlan) {
        try {
            LOGGER.debug("Performing predictive governance check for action plan: {}", actionPlan.id());
            List<Thought> simulations = ucr.simulate(actionPlan, UnifiedCausalReasoner.ReasoningOptions.simulation());

            for (Thought simulation : simulations) {
                // If the simulation indicates a potential problem (e.g., low clarity, explicit violation)
                if (simulation.state().clarity() < 0.7 || (simulation.content().text() != null && simulation.content().text().toLowerCase().contains("violation"))) {
                    String reason = "Predictive governance check detected potential issues: " + simulation.content().text();
                    LOGGER.warn("VETOED by predictive governance: {}", reason);
                    return Optional.of(reason);
                }
            }
            return Optional.empty(); // No issues detected
        } catch (Exception e) {
            LOGGER.error("Error during predictive governance check. Continuing with other checks.", e);
            return Optional.empty();
        }
    }

    /**
     * Uses an LLM to vet the action plan against a set of constitutional principles.
     */
    private Optional<String> performConstitutionalVetting(Thought actionPlan) {
        try {
            String planText = actionPlan.content().text();
            if (planText == null || planText.isBlank()) {
                return Optional.empty(); // No text to vet
            }

            String prompt = String.format(
                    """
                            You are a safety officer for an AI system. Your task is to determine if a proposed action plan violates any of the system's core principles.

                            ## Core Principles ##
                            %s

                            ## Proposed Action Plan ##
                            "%s"

                            Does the proposed action plan violate any of these principles? Answer with a single word, YES or NO. If YES, provide a brief, one-sentence explanation.""",
                    this.constitutionalPrinciples,
                    planText
            );

            LOGGER.debug("Sending plan to constitutional vetting model: {}", planText);
            Response<AiMessage> response = vettingModel.generate(UserMessage.from(prompt));
            String responseText = response.content().text().trim();
            LOGGER.debug("Received vetting response: {}", responseText);

            if (responseText.toUpperCase().startsWith("YES")) {
                String reason = extractVetoReason(responseText).orElse("The action violates one or more constitutional principles.");
                LOGGER.warn("VETOED by constitutional model: {}", reason);
                return Optional.of(reason);
            }

            return Optional.empty(); // Plan is approved
        } catch (Exception e) {
            LOGGER.error("Error during constitutional vetting. Vetoing plan as a fallback safety measure.", e);
            return Optional.of("Vetoed due to an internal error in the constitutional vetting model: " + e.getMessage());
        }
    }

    /**
     * Extracts the reason for a veto from the LLM's response.
     */
    private Optional<String> extractVetoReason(String responseText) {
        Matcher matcher = VETO_REASON_PATTERN.matcher(responseText);
        if (matcher.find()) {
            String reason = matcher.group(1).trim();
            if (!reason.isEmpty()) {
                return Optional.of(reason);
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a new rule to the governance system.
     */
    public void addRule(Rule rule) {
        this.rules.add(rule);
    }
}