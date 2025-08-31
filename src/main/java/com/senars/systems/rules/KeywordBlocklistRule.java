package com.senars.systems.rules;

import com.senars.core.Thought;
import com.senars.systems.Rule;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A rule that vetoes an action plan if its content contains any blocked keywords.
 */
public class KeywordBlocklistRule implements Rule {

    private final List<String> blockedKeywords;

    public KeywordBlocklistRule(List<String> blockedKeywords) {
        this.blockedKeywords = Objects.requireNonNull(blockedKeywords);
    }

    @Override
    public Optional<String> check(Thought actionPlan) {
        if (actionPlan == null || actionPlan.content() == null || actionPlan.content().text() == null) {
            return Optional.empty();
        }

        String text = actionPlan.content().text().toLowerCase();
        for (String keyword : blockedKeywords) {
            if (text.contains(keyword.toLowerCase())) {
                return Optional.of("Action plan vetoed due to presence of blocked keyword: " + keyword);
            }
        }

        return Optional.empty();
    }
}
