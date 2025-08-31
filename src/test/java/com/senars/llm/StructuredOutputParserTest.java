package com.senars.llm;

import com.senars.core.Thought;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;


class StructuredOutputParserTest {

    private StructuredOutputParser outputParser;

    @BeforeEach
    void setUp() {
        outputParser = new StructuredOutputParser();
    }

    @Test
    void testParseWithPlaceholderImplementation() {
        // Arrange
        String llmResponse = "This is the LLM's response.";

        // Act
        List<Thought> thoughts = outputParser.parse(llmResponse);

        // Assert
        assertNotNull(thoughts);
        assertFalse(thoughts.isEmpty());
        assertEquals(1, thoughts.size());

        Thought thought = thoughts.get(0);
        assertEquals(ThoughtType.REPORT, thought.metadata().type());
        assertEquals(llmResponse, thought.content().text());
    }

    @Test
    void testParseWithValidJson() {
        // Arrange
        String jsonResponse = "{\n" +
                "  \"type\": \"BELIEF\",\n" +
                "  \"content\": \"The sky is blue.\",\n" +
                "  \"clarity\": 0.95,\n" +
                "  \"salience\": 75.0\n" +
                "}";

        // Act
        List<Thought> thoughts = outputParser.parse(jsonResponse);

        // Assert
        assertNotNull(thoughts);
        assertEquals(1, thoughts.size());

        Thought thought = thoughts.get(0);
        assertEquals(ThoughtType.BELIEF, thought.metadata().type());
        assertEquals("The sky is blue.", thought.content().text());
        assertEquals(0.95, thought.state().clarity(), 0.001);
        assertEquals(75.0, thought.state().salience(), 0.001);
        assertEquals(ThoughtOrigin.LLM_INFERENCE, thought.metadata().origin());
    }

    @Test
    void testParseWithInvalidJson() {
        // Arrange
        String invalidJson = "this is not json";

        // Act
        List<Thought> thoughts = outputParser.parse(invalidJson);

        // Assert
        // Should fall back to creating a simple REPORT thought
        assertNotNull(thoughts);
        assertEquals(1, thoughts.size());
        assertEquals(ThoughtType.REPORT, thoughts.get(0).metadata().type());
        assertEquals(invalidJson, thoughts.get(0).content().text());
    }
}
