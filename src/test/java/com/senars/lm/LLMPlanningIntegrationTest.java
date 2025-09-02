package com.senars.lm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.cycle.Inference;
import com.senars.db.DatabaseManager;
import com.senars.systems.Memory;
import com.senars.systems.immemory.InMemoryMemory;
import com.senars.xai.Explain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LLMPlanningIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @TempDir
    Path tempDir;
    private Memory memory;
    private ChatLanguageModel chatModel;
    private Langchain4JCognition cognition;
    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() throws IOException {
        Path dbFile = tempDir.resolve("test-planning.db");
        dbManager = new DatabaseManager(dbFile);
        memory = new InMemoryMemory(AppConfig.getInstance(), dbManager);
        chatModel = mock(ChatLanguageModel.class);
        Sessions sessions = mock(Sessions.class);
        Explain explain = mock(Explain.class);
        ToolKit toolKit = mock(ToolKit.class);

        // Load schemas into memory
        loadSchema("planning-schema.json");
        loadSchema("parsing-schema.json");


        Inference inference = new Inference(memory);
        cognition = new Langchain4JCognition(
                chatModel,
                memory,
                new PromptBuilder(),
                new StructuredOutputParser(),
                sessions,
                explain,
                inference,
                toolKit
        );
    }

    private void loadSchema(String schemaName) throws IOException {
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaName)) {
            assertNotNull(schemaStream, schemaName + " not found in resources");
            List<Thought> schemas = objectMapper.readValue(schemaStream, new TypeReference<>() {
            });
            for (Thought schema : schemas) {
                memory.saveThought(schema);
            }
        }
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    void testGoalDecompositionIntoActionPlans() {
        // 1. Define the high-level GOAL
        Thought goal = new Thought(
                "goal-123",
                new ThoughtContent("Make a cup of tea.", null, List.of(0.1, 0.2, 0.3), null, null, null, null),
                new ThoughtState(1.0, 100.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now())
        );
        memory.saveThought(goal);

        // 2. Define the expected LLM response (a JSON array of action plans)
        String llmPlanResponse = """
                [
                  {
                    "id": "action-1",
                    "content": { "text": "Boil water." },
                    "state": { "clarity": 1.0, "salience": 100.0, "activation": 1.0 },
                    "metadata": { "type": "ACTION", "origin": "LLM_INFERENCE", "trace": ["goal-123"] }
                  },
                  {
                    "id": "action-2",
                    "content": { "text": "Get a cup and a tea bag." },
                    "state": { "clarity": 1.0, "salience": 100.0, "activation": 1.0 },
                    "metadata": { "type": "ACTION", "origin": "LLM_INFERENCE", "trace": ["goal-123"] }
                  },
                  {
                    "id": "action-3",
                    "content": { "text": "Pour water into the cup with the tea bag." },
                    "state": { "clarity": 1.0, "salience": 100.0, "activation": 1.0 },
                    "metadata": { "type": "ACTION", "origin": "LLM_INFERENCE", "trace": ["goal-123"] }
                  }
                ]""";

        // 3. Mock the ChatLanguageModel to return the unparsed plan for the first cycle,
        // and the parsed plan for the second (parsing) cycle.
        when(chatModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from(llmPlanResponse))) // Cycle 1: LLM returns a plan that needs parsing.
                .thenReturn(Response.from(AiMessage.from(llmPlanResponse))); // Cycle 2: LLM "parses" the text by returning the clean JSON.

        // 4. Process the GOAL thought (Cycle 1)
        List<Thought> firstResult = cognition.process(goal);

        // 5. Assert the first result is a "parse" goal
        assertNotNull(firstResult);
        assertEquals(1, firstResult.size());
        Thought parseGoal = firstResult.getFirst();
        assertEquals(ThoughtType.GOAL, parseGoal.metadata().type());
        assertEquals("senars:parse_text", parseGoal.content().symbolic());
        assertEquals(llmPlanResponse, parseGoal.content().text()); // Check that the text to parse is correct.

        // 6. Process the "parse" GOAL thought (Cycle 2)
        List<Thought> finalResult = cognition.process(parseGoal);


        // 7. Assert the final results
        assertNotNull(finalResult);
        assertEquals(3, finalResult.size(), "Should produce three action plan steps.");

        // Check the type of each thought
        assertTrue(finalResult.stream().allMatch(t -> t.metadata().type() == ThoughtType.ACTION),
                "All resulting thoughts should be of type ACTION.");

        // Check the content of each thought
        assertEquals("Boil water.", finalResult.get(0).content().text());
        assertEquals("Get a cup and a tea bag.", finalResult.get(1).content().text());
        assertEquals("Pour water into the cup with the tea bag.", finalResult.get(2).content().text());

        // Check that the trace is correct
        assertEquals("goal-123", finalResult.get(0).metadata().trace().getFirst());
    }
}
