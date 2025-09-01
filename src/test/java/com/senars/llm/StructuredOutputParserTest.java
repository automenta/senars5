package com.senars.llm;

import com.senars.core.Thought;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class StructuredOutputParserTest {

    private StructuredOutputParser outputParser;

    @BeforeEach
    void setUp() {
        outputParser = new StructuredOutputParser();
    }

    @Test
    void testParseWithInvalidJson() {
        // Arrange
        String invalidJson = "this is not json {";

        // Act
        List<Thought> thoughts = outputParser.parse(invalidJson);

        // Assert
        // Should fall back to creating a simple REPORT thought
        assertNotNull(thoughts);
        assertEquals(1, thoughts.size());
        assertEquals(ThoughtType.REPORT, thoughts.getFirst().metadata().type());
        assertEquals(invalidJson, thoughts.getFirst().content().text());
    }

    @Test
    void testParseWithEmptyJsonArray() {
        // Arrange
        String jsonResponse = "[]";

        // Act
        List<Thought> thoughts = outputParser.parse(jsonResponse);

        // Assert
        assertNotNull(thoughts);
        assertTrue(thoughts.isEmpty());
    }

    @Test
    void testParseWithComplexJsonArray() {
        // Arrange
        String jsonResponse = """
                [
                  {
                    "id": "thought-1",
                    "content": {
                      "text": "First thought text.",
                      "symbolic": "symbol-1"
                    },
                    "state": {
                      "clarity": 0.9,
                      "salience": 90.0,
                      "activation": 0.95
                    },
                    "metadata": {
                      "type": "BELIEF",
                      "origin": "LLM_INFERENCE",
                      "trace": ["focus-thought-id"],
                      "timestamp": "2025-01-01T12:00:00Z"
                    }
                  },
                  {
                    "id": "thought-2",
                    "content": {
                      "text": "Second thought, a goal."
                    },
                    "state": {
                      "clarity": 0.98,
                      "salience": 150.0,
                      "activation": 1.0
                    },
                    "metadata": {
                      "type": "GOAL",
                      "origin": "USER",
                      "trace": ["thought-1"],
                      "timestamp": "2025-01-01T12:01:00Z"
                    }
                  }
                ]""";

        // Act
        List<Thought> thoughts = outputParser.parse(jsonResponse);

        // Assert
        assertNotNull(thoughts);
        assertEquals(2, thoughts.size());

        // Verify first thought
        Thought first = thoughts.get(0);
        assertEquals("thought-1", first.id());
        assertEquals("First thought text.", first.content().text());
        assertEquals("symbol-1", first.content().symbolic());
        assertEquals(0.9, first.state().clarity());
        assertEquals(90.0, first.state().salience());
        assertEquals(0.95, first.state().activation());
        assertEquals(ThoughtType.BELIEF, first.metadata().type());
        assertEquals(ThoughtOrigin.LLM_INFERENCE, first.metadata().origin());
        assertEquals(List.of("focus-thought-id"), first.metadata().trace());
        assertEquals(Instant.parse("2025-01-01T12:00:00Z"), first.metadata().timestamp());


        // Verify second thought
        Thought second = thoughts.get(1);
        assertEquals("thought-2", second.id());
        assertEquals("Second thought, a goal.", second.content().text());
        assertNull(second.content().symbolic());
        assertEquals(0.98, second.state().clarity());
        assertEquals(150.0, second.state().salience());
        assertEquals(1.0, second.state().activation());
        assertEquals(ThoughtType.GOAL, second.metadata().type());
        assertEquals(ThoughtOrigin.USER, second.metadata().origin());
        assertEquals(List.of("thought-1"), second.metadata().trace());
        assertEquals(Instant.parse("2025-01-01T12:01:00Z"), second.metadata().timestamp());
    }
}
