package com.senars;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.senars.core.*;
import com.senars.llm.Langchain4JCognition;
import com.senars.llm.PromptBuilder;
import com.senars.llm.StructuredOutputParser;
import com.senars.systems.Memory;
import com.senars.systems.immemory.InMemoryMemory;
import com.senars.core.Sessions;
import com.senars.cycle.Inference;
import com.senars.xai.Explain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlanningIntegrationTest {

    private Memory memory;
    private ChatLanguageModel chatModel;
    private Langchain4JCognition cognition;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() throws IOException {
        memory = new InMemoryMemory();
        chatModel = mock(ChatLanguageModel.class);
        Sessions sessions = mock(Sessions.class);
        Explain explain = mock(Explain.class);

        // Load the planning schema into memory
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("planning-schema.json")) {
            assertNotNull(schemaStream, "planning-schema.json not found in resources");
            List<Thought> schemas = objectMapper.readValue(schemaStream, new TypeReference<List<Thought>>() {});
            for (Thought schema : schemas) {
                memory.saveThought(schema);
            }
        }

        Inference inference = new Inference(memory);
        cognition = new Langchain4JCognition(
                chatModel,
                memory,
                new PromptBuilder(),
                new StructuredOutputParser(),
                sessions,
                explain,
                inference
        );
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
        String expectedLlMResponse = """
                [
                  {
                    "id": "action-1",
                    "content": { "text": "Boil water." },
                    "state": { "clarity": 1.0, "salience": 100.0, "activation": 1.0 },
                    "metadata": { "type": "ACTION_PLAN", "origin": "LLM_INFERENCE", "trace": ["goal-123"] }
                  },
                  {
                    "id": "action-2",
                    "content": { "text": "Get a cup and a tea bag." },
                    "state": { "clarity": 1.0, "salience": 100.0, "activation": 1.0 },
                    "metadata": { "type": "ACTION_PLAN", "origin": "LLM_INFERENCE", "trace": ["goal-123"] }
                  },
                  {
                    "id": "action-3",
                    "content": { "text": "Pour water into the cup with the tea bag." },
                    "state": { "clarity": 1.0, "salience": 100.0, "activation": 1.0 },
                    "metadata": { "type": "ACTION_PLAN", "origin": "LLM_INFERENCE", "trace": ["goal-123"] }
                  }
                ]""";

        // 3. Mock the ChatLanguageModel to return the expected response
        when(chatModel.generate(any(dev.langchain4j.data.message.UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from(expectedLlMResponse)));

        // 4. Process the GOAL thought
        List<Thought> resultingThoughts = cognition.process(goal);

        // 5. Assert the results
        assertNotNull(resultingThoughts);
        assertEquals(3, resultingThoughts.size(), "Should produce three action plan steps.");

        // Check the type of each thought
        assertTrue(resultingThoughts.stream().allMatch(t -> t.metadata().type() == ThoughtType.ACTION_PLAN),
                "All resulting thoughts should be of type ACTION_PLAN.");

        // Check the content of each thought
        assertEquals("Boil water.", resultingThoughts.get(0).content().text());
        assertEquals("Get a cup and a tea bag.", resultingThoughts.get(1).content().text());
        assertEquals("Pour water into the cup with the tea bag.", resultingThoughts.get(2).content().text());

        // Check that the trace is correct
        assertEquals("goal-123", resultingThoughts.get(0).metadata().trace().get(0));
    }
}
