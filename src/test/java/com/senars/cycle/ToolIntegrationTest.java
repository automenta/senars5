package com.senars.cycle;

import com.senars.core.*;
import com.senars.lm.ToolKit;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import com.senars.tools.LogicalInferenceTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ToolIntegrationTest {

    @Mock
    private Memory memory;
    @Mock
    private dev.langchain4j.model.chat.ChatLanguageModel chatModel;
    @Mock
    private com.senars.lm.PromptBuilder promptBuilder;
    @Mock
    private com.senars.lm.StructuredOutputParser outputParser;
    @Mock
    private com.senars.explain.Explain explain;
    @Mock
    private com.senars.events.EventBus eventBus;


    private ToolKit toolKit;
    private Action toolUsingAction;
    private LogicalInferenceTool logicalInferenceTool;

    @BeforeEach
    void setUp() {
        LogicEngine logicEngine = new LogicEngine();
        Inference inference = new Inference(memory, logicEngine);
        logicalInferenceTool = new LogicalInferenceTool(inference);
        toolKit = new ToolKit(logicalInferenceTool); // In a real scenario, more tools would be here.
        toolUsingAction = new ToolUsingAction(toolKit);
    }

    private Thought createActionPlan(String toolRequestJson) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Execute tool", toolRequestJson, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.LLM_INFERENCE, emptyList(), Instant.now())
        );
    }

    @Test
    void testLogicalInferenceTool_Success() {
        // Arrange
        String query = "father('darth_vader', 'luke')";
        String toolRequestJson = String.format("{\"name\":\"executeQuery\",\"arguments\":{\"query\":\"%s\"}}", query);
        Thought actionPlan = createActionPlan(toolRequestJson);

        Thought fact = new Thought(
                "fact-1",
                new ThoughtContent("Darth Vader is Luke's father.", query + ".", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, emptyList(), Instant.now())
        );
        when(memory.getAllThoughts()).thenReturn(List.of(fact));

        // Act
        Feedback feedback = toolUsingAction.executePlan(actionPlan);

        // Assert
        assertEquals(ActionStatus.SUCCESS, feedback.status());
        assertEquals("executeQuery", feedback.toolName());
        assertEquals("Fact is true.", feedback.output());
    }

    @Test
    void testLogicalInferenceTool_Failure_NoSolutions() {
        // Arrange
        String query = "father('emperor_palpatine', 'luke')";
        String toolRequestJson = String.format("{\"name\":\"executeQuery\",\"arguments\":{\"query\":\"%s\"}}", query);
        Thought actionPlan = createActionPlan(toolRequestJson);

        Thought fact = new Thought(
                "fact-1",
                new ThoughtContent("Darth Vader is Luke's father.", "father('darth_vader', 'luke').", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, emptyList(), Instant.now())
        );
        when(memory.getAllThoughts()).thenReturn(List.of(fact));

        // Act
        Feedback feedback = toolUsingAction.executePlan(actionPlan);

        // Assert
        assertEquals(ActionStatus.FAILURE, feedback.status());
        assertEquals("executeQuery", feedback.toolName());
        assertEquals("Error: Query yielded no solutions.", feedback.output());
    }

    @Test
    void testLogicalInferenceTool_Failure_EmptyKnowledgeBase() {
        // Arrange
        String query = "father(X, 'luke')";
        String toolRequestJson = String.format("{\"name\":\"executeQuery\",\"arguments\":{\"query\":\"%s\"}}", query);
        Thought actionPlan = createActionPlan(toolRequestJson);

        when(memory.getAllThoughts()).thenReturn(emptyList());

        // Act
        Feedback feedback = toolUsingAction.executePlan(actionPlan);

        // Assert
        assertEquals(ActionStatus.FAILURE, feedback.status());
        assertEquals("executeQuery", feedback.toolName());
        assertEquals("Error: The knowledge base is empty. No facts or rules are available.", feedback.output());
    }
}
