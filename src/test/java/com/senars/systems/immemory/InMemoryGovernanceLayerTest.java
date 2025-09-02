package com.senars.systems.immemory;

import com.senars.core.*;
import com.senars.systems.Rule;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InMemoryGovernanceLayerTest {

    @Mock
    private ChatLanguageModel vettingModel;

    private static final String FAKE_CONSTITUTION = "Be nice.";

    private Thought createActionPlan(String text) {
        return new Thought("plan-id", new ThoughtContent(text, null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.LLM_INFERENCE, List.of(), null));
    }

    @Test
    void reviewPlan_withNoRules_shouldApprove() {
        when(vettingModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("NO")));
        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(), FAKE_CONSTITUTION, vettingModel);
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
        when(vettingModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("NO")));

        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(rule1, rule2), FAKE_CONSTITUTION, vettingModel);
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
        // rule2 and vetting model are not even called

        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(rule1, rule2), FAKE_CONSTITUTION, vettingModel);
        Thought plan = createActionPlan("Do something questionable.");
        Optional<String> result = governanceLayer.reviewPlan(plan);

        assertTrue(result.isPresent());
        assertEquals(vetoReason, result.get());
    }

    @Test
    void reviewPlan_withConstitutionalVeto_shouldVeto() {
        String vetoReason = "The action is not nice.";
        when(vettingModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("YES, because " + vetoReason)));

        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(), FAKE_CONSTITUTION, vettingModel);
        Thought plan = createActionPlan("Be mean to the user.");
        Optional<String> result = governanceLayer.reviewPlan(plan);

        assertTrue(result.isPresent());
        assertEquals("because The action is not nice.", result.get());
    }

    @Test
    void reviewPlan_withConstitutionalApproval_shouldApprove() {
        when(vettingModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("NO")));
        InMemoryGovernor governanceLayer = new InMemoryGovernor(List.of(), FAKE_CONSTITUTION, vettingModel);
        Thought plan = createActionPlan("Be very helpful and polite.");
        Optional<String> result = governanceLayer.reviewPlan(plan);

        assertTrue(result.isEmpty());
    }
}
