package com.senars.cycle;

import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.lm.ToolKit;
import com.senars.tools.LogicalInferenceTool;
import com.senars.systems.Memory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ToolIntegrationTest {

    @Mock private Memory memory;
    @Mock private dev.langchain4j.model.chat.ChatLanguageModel chatModel;
    @Mock private com.senars.lm.PromptBuilder promptBuilder;
    @Mock private com.senars.lm.StructuredOutputParser outputParser;
    @Mock private com.senars.core.Sessions sessions;
    @Mock private com.senars.xai.Explain explain;


    private ToolKit toolKit;
    private Action toolUsingAction;
    private LogicalInferenceTool logicalInferenceTool;
    private Cognition cognition;

    @BeforeEach
    void setUp() {
        logicalInferenceTool = new LogicalInferenceTool(memory);
        toolKit = new ToolKit(logicalInferenceTool); // In a real scenario, more tools would be here.
        toolUsingAction = new ToolUsingAction(toolKit);
        cognition = spy(new Langchain4JCognition(chatModel, memory, promptBuilder, outputParser, sessions, explain, toolKit));
    }

    private Thought createActionPlan(String toolRequestJson) {
        return new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Execute tool", toolRequestJson, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.LLM_INFERENCE, Collections.emptyList(), Instant.now())
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
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
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
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, Collections.emptyList(), Instant.now())
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

        when(memory.getAllThoughts()).thenReturn(Collections.emptyList());

        // Act
        Feedback feedback = toolUsingAction.executePlan(actionPlan);

        // Assert
        assertEquals(ActionStatus.FAILURE, feedback.status());
        assertEquals("executeQuery", feedback.toolName());
        assertEquals("Error: The knowledge base is empty. No facts or rules are available.", feedback.output());
    }

    @Test
    void testFailureRecoveryLoop() {
        // Arrange: Create a failure goal, similar to what Grounding would create.
        String failureText = "Investigate and resolve failure of tool 'executeQuery'. Error: Query yielded no solutions.";
        Thought failureGoal = new Thought(
                "failure-goal-1",
                new ThoughtContent(failureText, null, List.of(1.0, 2.0, 3.0), null, null, null, null),
                new ThoughtState(1.0, 100.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.SYSTEM, Collections.emptyList(), Instant.now())
        );

        // Arrange: Mock the memory to return the Failure Recovery Schema when requested.
        Thought recoverySchema = com.senars.core.Genesis.createFailureRecoverySchema(new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel());
        when(memory.findSchemaBySymbolicName(com.senars.core.Genesis.FAILURE_RECOVERY_SCHEMA_SYMBOL))
                .thenReturn(java.util.Optional.of(recoverySchema));

        // Arrange: Mock the LLM to return a new action plan (e.g., to use a search tool)
        String newActionJson = "{\"name\":\"search\",\"arguments\":{\"query\":\"who is luke's father\"}}";
        when(chatModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(dev.langchain4j.model.output.Response.from(dev.langchain4j.data.message.AiMessage.from(newActionJson)));

        // Act: Process the failure goal.
        List<Thought> newThoughts = cognition.process(failureGoal);

        // Assert
        // 1. Verify that the findRelevantSchema method was called and returned our recovery schema.
        verify((Langchain4JCognition)cognition).findRelevantSchema(failureGoal);

        // 2. Assert that the result is a single new thought.
        assertEquals(1, newThoughts.size());
        Thought newActionPlan = newThoughts.getFirst();

        // 3. Assert that the new thought is an ACTION plan.
        assertEquals(ThoughtType.ACTION, newActionPlan.metadata().type());

        // 4. Assert that the action plan is for the new tool (search).
        ToolExecutionRequest newRequest = toolKit.parse(newActionPlan.content().symbolic());
        assertNotNull(newRequest);
        assertEquals("search", newRequest.name());
    }
}
