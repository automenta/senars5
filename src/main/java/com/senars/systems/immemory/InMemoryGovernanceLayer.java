package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.systems.IGovernanceLayer;

import java.util.Optional;

import com.senars.systems.Rule;

import java.util.List;
import java.util.Objects;

/**
 * An in-memory implementation of the IGovernanceLayer.
 * It uses a set of configurable rules to review action plans and veto any
 * that are deemed unsafe or violate core directives.
 */
public class InMemoryGovernanceLayer implements IGovernanceLayer {

    private final List<Rule> rules;

    public InMemoryGovernanceLayer(List<Rule> rules) {
        this.rules = Objects.requireNonNull(rules);
    }

    @Override
    public Optional<String> reviewPlan(Thought actionPlan) {
        for (Rule rule : rules) {
            Optional<String> vetoReason = rule.check(actionPlan);
            if (vetoReason.isPresent()) {
                return vetoReason; // Veto immediately if a rule is violated
            }
        }
        return Optional.empty(); // All rules passed, approve the plan
    }
}
