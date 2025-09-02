package com.senars.logic;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaCognitiveServiceTest {

    @Mock
    private ChatLanguageModel languageModel;

    @InjectMocks
    private MetaCognitiveService metaCognitiveService;

    @Test
    void testAnalyzeCognitiveStall() throws ExecutionException, InterruptedException {
        // Arrange
        List<Thought> history = List.of(new Thought("t1", null, null, null));
        String llmResponseJson = "```json\n{\"analysis\": \"...\", \"new_goal\": \"This is the new goal to break the stall.\"}\n```";
        Response<AiMessage> llmResponse = Response.from(AiMessage.from(llmResponseJson));

        when(languageModel.generate(any(UserMessage.class))).thenReturn(llmResponse);

        // Act
        Thought result = metaCognitiveService.analyzeCognitiveStall(history).get();

        // Assert
        assertNotNull(result);
        assertEquals(ThoughtType.GOAL, result.metadata().type());
        assertEquals("This is the new goal to break the stall.", result.content().text());
        assertEquals("system_unstuck_resolution", result.content().symbolic());
    }

    @Test
    void testAnalyzeSystemError() throws ExecutionException, InterruptedException {
        // Arrange
        Throwable error = new RuntimeException("Test Exception");
        String llmResponseJson = "{\"recovery_goal\": \"This is the recovery goal for the error.\"}";
        Response<AiMessage> llmResponse = Response.from(AiMessage.from(llmResponseJson));

        when(languageModel.generate(any(UserMessage.class))).thenReturn(llmResponse);

        // Act
        Thought result = metaCognitiveService.analyzeSystemError(error).get();

        // Assert
        assertNotNull(result);
        assertEquals(ThoughtType.GOAL, result.metadata().type());
        assertEquals("This is the recovery goal for the error.", result.content().text());
        assertEquals("system_error_recovery", result.content().symbolic());
    }

    @Test
    void testAnalyzeCognitiveStall_JsonParseFailure() throws ExecutionException, InterruptedException {
        // Arrange
        List<Thought> history = List.of(new Thought("t1", null, null, null));
        String llmResponseText = "This is not valid JSON"; // Invalid response
        Response<AiMessage> llmResponse = Response.from(AiMessage.from(llmResponseText));

        when(languageModel.generate(any(UserMessage.class))).thenReturn(llmResponse);

        // Act
        Thought result = metaCognitiveService.analyzeCognitiveStall(history).get();

        // Assert
        assertNotNull(result);
        assertEquals("The system is stuck. Formulate a plan to get unstuck.", result.content().text());
        assertEquals("fallback_unstuck", result.content().symbolic());
    }
}
