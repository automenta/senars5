package com.senars.llm;

import com.senars.core.Thought;
import com.senars.cycle.ICognitiveProcessor;
import com.senars.systems.IMemoryNexus;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Langchain4jCognitiveProcessorTest {

    @Mock
    private ChatModel mockChatModel;
    @Mock
    private IMemoryNexus mockMemoryNexus;
    @Mock
    private PromptBuilder mockPromptBuilder;
    @Mock
    private StructuredOutputParser mockOutputParser;
    @Mock
    private Thought mockFocusThought;
    @Mock
    private Thought mockResultThought;

    private ICognitiveProcessor cognitiveProcessor;

    @BeforeEach
    void setUp() {
        cognitiveProcessor = new Langchain4jCognitiveProcessor(
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
        String expectedResponse = "This is the LLM response.";
        List<Thought> expectedThoughts = List.of(mockResultThought);

        when(mockFocusThought.id()).thenReturn("test-id");
        when(mockPromptBuilder.build(null, mockFocusThought, Collections.emptyList())).thenReturn(expectedPrompt);
        when(mockChatModel.chat(expectedPrompt)).thenReturn(expectedResponse);
        when(mockOutputParser.parse(expectedResponse)).thenReturn(expectedThoughts);

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert
        assertNotNull(result);
        assertEquals(expectedThoughts, result);

        // Verify that the collaborators were called in the correct order with the correct parameters
        verify(mockPromptBuilder, times(1)).build(null, mockFocusThought, Collections.emptyList());
        verify(mockChatModel, times(1)).chat(expectedPrompt);
        verify(mockOutputParser, times(1)).parse(expectedResponse);
        verifyNoInteractions(mockMemoryNexus); // MemoryNexus is not used yet
    }
}
