package com.senars.llm;

import com.senars.core.Thought;
import com.senars.cycle.Cognition;
import com.senars.systems.Memory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Langchain4jCognitiveProcessorTest {

    @Mock
    private ChatLanguageModel mockChatModel;
    @Mock
    private Memory mockMemoryNexus;
    @Mock
    private PromptBuilder mockPromptBuilder;
    @Mock
    private StructuredOutputParser mockOutputParser;
    @Mock
    private Thought mockFocusThought;
    @Mock
    private Thought mockResultThought;

    private Cognition cognitiveProcessor;

    @BeforeEach
    void setUp() {
        cognitiveProcessor = new Langchain4JCognition(
                mockChatModel,
                mockMemoryNexus,
                mockPromptBuilder,
                mockOutputParser
        );
    }

    @Test
    void processShouldOrchestrateCallsToCollaborators() {
        // Arrange
        String expectedPrompt = "This is a test prompt.";
        String expectedResponseText = "This is the LLM response.";
        List<Thought> expectedThoughts = List.of(mockResultThought);
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(expectedResponseText));

        when(mockFocusThought.id()).thenReturn("test-id");
        when(mockPromptBuilder.build(null, mockFocusThought, Collections.emptyList())).thenReturn(expectedPrompt);
        when(mockChatModel.generate(ArgumentMatchers.<UserMessage>any())).thenReturn(mockResponse);
        when(mockOutputParser.parse(expectedResponseText)).thenReturn(expectedThoughts);

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert
        assertNotNull(result);
        assertEquals(expectedThoughts, result);

        // Verify that the collaborators were called in the correct order with the correct parameters
        verify(mockPromptBuilder, times(1)).build(null, mockFocusThought, Collections.emptyList());
        verify(mockChatModel, times(1)).generate(ArgumentMatchers.<UserMessage>any());
        verify(mockOutputParser, times(1)).parse(expectedResponseText);
        verifyNoInteractions(mockMemoryNexus); // MemoryNexus is not used yet
    }
}
