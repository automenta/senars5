package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.systems.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InMemoryGovernanceLayerTest {

    private Thought createActionPlan(String text) {
        return new Thought("plan-id", new ThoughtContent(text, null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.LLM_INFERENCE, List.of(), null));
    }

    @Test
    void reviewPlan_withNoRules_shouldApprove() {
        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of());
        Thought plan = createActionPlan("Do something benign.");
        Optional<String> result = governanceLayer.reviewPlan(plan);
        assertTrue(result.isEmpty());
    }

    @Test
    void reviewPlan_withPassingRules_shouldApprove() {
        Rule rule1 = mock(Rule.class);
        Rule rule2 = mock(Rule.class);
        when(rule1.check(any())).thenReturn(Optional.empty());
        when(rule2.check(any())).thenReturn(Optional.empty());

        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(rule1, rule2));
        Thought plan = createActionPlan("Do something benign.");
        Optional<String> result = governanceLayer.reviewPlan(plan);

        assertTrue(result.isEmpty());
    }

    @Test
    void reviewPlan_withOneVetoingRule_shouldVeto() {
        String vetoReason = "Vetoed by rule 1";
        Rule rule1 = mock(Rule.class);
        Rule rule2 = mock(Rule.class);
        when(rule1.check(any())).thenReturn(Optional.of(vetoReason));
        // rule2 is not even called

        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(rule1, rule2));
        Thought plan = createActionPlan("Do something questionable.");
        Optional<String> result = governanceLayer.reviewPlan(plan);

        assertTrue(result.isPresent());
        assertEquals(vetoReason, result.get());
    }
}
