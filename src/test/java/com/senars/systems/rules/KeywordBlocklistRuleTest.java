package com.senars.systems.rules;

import com.senars.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordBlocklistRuleTest {

    private Thought createActionPlan(String text) {
        return new Thought("plan-id", new ThoughtContent(text, null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION_PLAN, ThoughtOrigin.LLM_INFERENCE, List.of(), null));
    }

    @Test
    void check_withBlockedKeyword_shouldVeto() {
        KeywordBlocklistRule rule = new KeywordBlocklistRule(List.of("delete", "shutdown"));
        Thought plan = createActionPlan("I will shutdown the system.");
        Optional<String> result = rule.check(plan);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("shutdown"));
    }

    @Test
    void check_withNoBlockedKeywords_shouldApprove() {
        KeywordBlocklistRule rule = new KeywordBlocklistRule(List.of("delete", "shutdown"));
        Thought plan = createActionPlan("I will restart the server.");
        Optional<String> result = rule.check(plan);
        assertTrue(result.isEmpty());
    }

    @Test
    void check_withCaseInsensitiveKeyword_shouldVeto() {
        KeywordBlocklistRule rule = new KeywordBlocklistRule(List.of("DELETE"));
        Thought plan = createActionPlan("I will delete the files.");
        Optional<String> result = rule.check(plan);
        assertTrue(result.isPresent());
    }

    @Test
    void check_withNullContent_shouldApprove() {
        KeywordBlocklistRule rule = new KeywordBlocklistRule(List.of("delete"));
        Thought plan = new Thought("id", new ThoughtContent(null, null, null, null, null, null, null), null, null);
        Optional<String> result = rule.check(plan);
        assertTrue(result.isEmpty());
    }
}
