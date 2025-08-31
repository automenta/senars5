package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.systems.IGovernanceLayer;

import java.util.Optional;

/**
 * An in-memory implementation of the IGovernanceLayer that approves all plans.
 * Useful for development and testing when safety checks are not required.
 */
public class PassThroughGovernanceLayer implements IGovernanceLayer {

    @Override
    public Optional<String> reviewPlan(Thought actionPlan) {
        // Always approve the plan by returning an empty Optional.
        return Optional.empty();
    }
}
