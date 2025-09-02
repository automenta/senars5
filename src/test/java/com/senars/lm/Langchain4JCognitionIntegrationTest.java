package com.senars.lm;

import com.senars.core.*;
import com.senars.systems.Memory;
import com.senars.systems.ScoredThought;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class Langchain4JCognitionIntegrationTest {

    @Mock
    private ChatLanguageModel chatModel;
    @Mock
    private Memory memory;
    @Mock
    private PromptBuilder promptBuilder;
    @Mock
    private StructuredOutputParser outputParser;
    @Mock
    private ToolKit toolKit;

    private Langchain4JCognition cognition;

    @Mock
    private com.senars.xai.Explain explain;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cognition = new Langchain4JCognition(chatModel, memory, promptBuilder, outputParser, explain, toolKit);
    }

    @Test
    void findRelevantSchema_shouldPreferSchemaWithHigherClarity() {
        // GIVEN
        List<Double> embedding = List.of(1.0, 0.0);

        Thought schemaA = new Thought(
                "schema-a",
                new ThoughtContent("Schema A", "schema-a", embedding, null, "prompt-a", null, null),
                new ThoughtState(1.0, 1.0, 1.0), // High clarity
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );

        Thought schemaB = new Thought(
                "schema-b",
                new ThoughtContent("Schema B", "schema-b", embedding, null, "prompt-b", null, null),
                new ThoughtState(0.2, 1.0, 1.0), // Low clarity
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );

        // Mock the memory response. Both have the same high similarity score.
        when(memory.retrieveSimilar(any(), any(Integer.class), eq(ThoughtType.SCHEMA)))
                .thenReturn(List.of(
                        new ScoredThought(schemaA, 0.95),
                        new ScoredThought(schemaB, 0.95)
                ));

        Thought focusThought = new Thought(
                "focus-1",
                new ThoughtContent("some goal", "goal", embedding, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now())
        );

        // WHEN
        Thought selectedSchema = cognition.findRelevantSchema(focusThought);

        // THEN
        assertNotNull(selectedSchema);
        assertEquals("schema-a", selectedSchema.id(), "Should select the schema with higher clarity");
    }

    @Test
    void parseResponse_shouldCreateReplanGoalOnParsingFailure() {
        // GIVEN
        String unparsableText = "This is not valid JSON.";
        Thought originalThought = new Thought("original-id", null, null, null);
        Thought parseGoal = new Thought(
                "parse-goal-id",
                new ThoughtContent(unparsableText, "senars:parse_text", null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.SYSTEM, List.of(originalThought.id()), Instant.now())
        );
        Thought parsingSchema = new Thought(
                "parsing-schema-id",
                new ThoughtContent(null, "senars:parse_text_schema", null, null, null, null, null),
                null, null
        );

        when(toolKit.parse(unparsableText)).thenReturn(null);
        when(outputParser.parse(unparsableText)).thenReturn(List.of()); // Simulate parsing failure

        // WHEN
        List<Thought> newThoughts = cognition.parseResponse(unparsableText, parseGoal, parsingSchema);

        // THEN
        assertEquals(1, newThoughts.size(), "Should create one new thought");
        Thought replanGoal = newThoughts.getFirst();
        assertEquals(ThoughtType.GOAL, replanGoal.metadata().type(), "The new thought should be a GOAL");
        assertEquals("senars:replan", replanGoal.content().symbolic(), "The goal should be a replan goal");
        assertTrue(replanGoal.metadata().trace().contains(originalThought.id()), "The replan goal should trace back to the original thought");
        assertTrue(replanGoal.content().text().contains(unparsableText), "The goal text should contain the failed response");
    }
}
