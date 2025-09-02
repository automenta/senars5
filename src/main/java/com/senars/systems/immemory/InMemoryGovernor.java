package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.systems.Governor;
import com.senars.systems.Rule;
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
 * An in-memory implementation of the Governance Layer.
 * It uses a two-stage process to review action plans:
 * 1. A fast, hard-coded check against a list of symbolic rules.
 * 2. A more nuanced, LM-based check against a set of constitutional principles.
 */
public class InMemoryGovernor implements Governor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryGovernor.class);
    private static final Pattern VETO_REASON_PATTERN = Pattern.compile("^YES(?:,|,\\s*(?:because|reason:))?\\s*(.*)", Pattern.CASE_INSENSITIVE);
    private final List<Rule> rules;
    private final String constitutionalPrinciples;
    private final ChatLanguageModel vettingModel;

    /**
     * Constructs a new InMemoryGovernor.
     *
     * @param rules                    A list of hard-coded rules for the first-stage check.
     * @param constitutionalPrinciples The text of the constitution for the second-stage LM check.
     * @param vettingModel             A dedicated, isolated ChatLanguageModel for vetting.
     */
    public InMemoryGovernor(List<Rule> rules, String constitutionalPrinciples, ChatLanguageModel vettingModel) {
        this.rules = Objects.requireNonNull(rules);
        this.constitutionalPrinciples = Objects.requireNonNull(constitutionalPrinciples);
        this.vettingModel = Objects.requireNonNull(vettingModel);
    }

    @Override
    public Optional<String> reviewPlan(Thought actionPlan) {
        // Stage 1: Core Directives Check (Fast, Symbolic)
        for (Rule rule : rules) {
            Optional<String> vetoReason = rule.check(actionPlan);
            if (vetoReason.isPresent()) {
                LOGGER.warn("VETOED by rule '{}': {}", rule.getClass().getSimpleName(), vetoReason.get());
                return vetoReason;
            }
        }

        // Stage 2: Constitutional LM Vetting (Nuanced, Semantic)
        return performConstitutionalVetting(actionPlan);
    }

    private Optional<String> performConstitutionalVetting(Thought actionPlan) {
        try {
            String planText = actionPlan.content().text();
            if (planText == null || planText.isBlank()) {
                return Optional.empty(); // No text to vet
            }

            String prompt = String.format(
                "You are a safety officer for an AI system. Your task is to determine if a proposed action plan violates any of the system's core principles.\n\n" +
                "## Core Principles ##\n%s\n\n" +
                "## Proposed Action Plan ##\n\"%s\"\n\n" +
                "Does the proposed action plan violate any of these principles? Answer with a single word, YES or NO. If YES, provide a brief, one-sentence explanation.",
                this.constitutionalPrinciples,
                planText
            );

            LOGGER.debug("Sending plan to constitutional vetting model: {}", planText);
            Response<dev.langchain4j.data.message.AiMessage> response = vettingModel.generate(UserMessage.from(prompt));
            String responseText = response.content().text().trim();
            LOGGER.debug("Received vetting response: {}", responseText);

            if (responseText.toUpperCase().startsWith("YES")) {
                String reason = extractVetoReason(responseText).orElse("The action violates one or more constitutional principles.");
                LOGGER.warn("VETOED by constitutional model: {}", reason);
                return Optional.of(reason);
            }

            return Optional.empty(); // Plan is approved
        } catch (Exception e) {
            LOGGER.error("Error during constitutional vetting. Approving plan as a fallback safety measure.", e);
            // Fallback to approve if the vetting model fails, to prevent the system from getting stuck.
            // A more robust implementation might trigger a high-priority error state.
            return Optional.empty();
        }
    }

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
}
