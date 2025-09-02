package com.senars.optimizer;

import com.senars.SystemFactory;
import com.senars.core.*;
import com.senars.cycle.CognitiveCycle;
import com.senars.cycle.ShutdownException;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OptimizerIntegrationTest {

    @TempDir
    Path tempDir;
    private SystemFactory systemFactory;
    private Memory memory;
    private LogicEngine logicEngine;
    private EventBus eventBus;
    private SchemaOptimizer schemaOptimizer;
    private CognitiveCycle cognitiveCycle;
    private ChatLanguageModel chatModel;

    @BeforeEach
    void setUp() {
        // We need to mock the LLM for this test
        chatModel = Mockito.mock(ChatLanguageModel.class);
        Path dbPath = tempDir.resolve("test.db");
        systemFactory = new SystemFactory(chatModel, dbPath);

        memory = systemFactory.memory;
        logicEngine = systemFactory.logicEngine;
        eventBus = systemFactory.eventBus;
        schemaOptimizer = systemFactory.schemaOptimizer;
        cognitiveCycle = systemFactory.getCognitiveCycle();
    }

    @Test
    @Disabled("This test is disabled due to a dependency conflict with kotlin-stdlib that causes a NoSuchMethodError.")
    void testFullSchemaHealingLoop() throws ShutdownException {
        // 1. Create a "bad" schema and add it to memory.
        String badSchemaId = "bad-schema-1";
        Thought badSchema = new Thought(
                badSchemaId,
                new ThoughtContent("A schema that always fails", "test:bad_schema", null, null, "This prompt is designed to be bad.", null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
        memory.saveThought(badSchema);

        // 2. Simulate 10 failed actions using this schema.
        for (int i = 0; i < 10; i++) {
            Thought action = new Thought(
                    "action-" + i,
                    new ThoughtContent("Do a thing", "some_tool()", null, null, null, null, null),
                    new ThoughtState(1.0, 1.0, 1.0),
                    new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.LLM_INFERENCE, List.of(badSchemaId), Instant.now())
            );
            Feedback feedback = new Feedback(ActionStatus.FAILURE, "some_tool", "it failed", 100L, action);
            eventBus.publish(new Events.ActionExecutedEvent(feedback));
        }

        // 3. Run the optimizer and assert that an optimization goal is created.
        List<Thought> goals = schemaOptimizer.run();
        goals.forEach(thought -> eventBus.publish(new Events.NewThoughtCreatedEvent(thought)));

        Optional<Thought> optimizationGoalOpt = memory.getAllThoughts().stream()
                .filter(t -> t.metadata().type() == ThoughtType.GOAL && t.content().text().contains("is inefficient because"))
                .findFirst();
        assertTrue(optimizationGoalOpt.isPresent(), "SchemaOptimizer should have created a goal.");
        Thought optimizationGoal = optimizationGoalOpt.get();
        assertEquals(badSchemaId, optimizationGoal.metadata().trace().getFirst());

        // 4. Mock the LLM's response for the optimization task.
        String newSchemaName = "A much better schema v2";
        String newProceduralContent = "This is the new and improved prompt that will definitely work.";
        String arguments = String.format(
                "{\"oldSchemaId\":\"%s\",\"newSchemaName\":\"%s\",\"newSchemaProceduralContent\":\"%s\"}",
                badSchemaId, newSchemaName, newProceduralContent
        );
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .name("rewriteSchema")
                .arguments(arguments)
                .build();

        when(chatModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(dev.langchain4j.model.output.Response.from(AiMessage.from(toolExecutionRequest)));

        // 5. Run the cognitive cycle to process the goal.
        for (int i = 0; i < 20; i++) {
            cognitiveCycle.step();
        }

        // 6. Assert that the old schema is deprecated.
        Thought deprecatedSchema = memory.getThoughtById(badSchemaId).orElseThrow();
        assertEquals(0.1, deprecatedSchema.state().clarity(), 0.001);
        assertFalse(logicEngine.solve(String.format("is_deprecated('%s')", badSchemaId)).isEmpty(), "Deprecated fact should be in logic engine");

        // 7. Assert that a new schema was created.
        Optional<Thought> newSchemaOpt = memory.getAllThoughts().stream()
                .filter(t -> t.metadata().type() == ThoughtType.SCHEMA && newSchemaName.equals(t.content().text()))
                .findFirst();
        assertTrue(newSchemaOpt.isPresent(), "A new schema should have been created.");
        Thought newSchema = newSchemaOpt.get();
        assertEquals(newProceduralContent, newSchema.content().procedural());
        assertEquals(badSchemaId, newSchema.metadata().trace().getFirst());

        // 8. Assert that an XAI report goal was created.
        AtomicReference<Boolean> xaiGoalFound = new AtomicReference<>(false);
        memory.getAllThoughts().stream()
                .filter(t -> t.metadata().type() == ThoughtType.GOAL && t.content().text().contains("Generate a human-readable report"))
                .findFirst()
                .ifPresent(g -> {
                    assertTrue(g.metadata().trace().contains(newSchema.id()));
                    xaiGoalFound.set(true);
                });
        assertTrue(xaiGoalFound.get(), "XAI report goal should have been created.");
    }
}
