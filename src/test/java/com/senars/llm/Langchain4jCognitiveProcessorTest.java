package com.senars.llm;

import com.senars.core.Sessions;
import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.cycle.Cognition;
import com.senars.systems.Memory;
import com.senars.xai.Explain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Langchain4jCognitiveProcessorTest {

    @Mock
    private ChatLanguageModel mockChatModel;
    @Mock
    private Memory mockMemory;
    @Mock
    private PromptBuilder mockPromptBuilder;
    @Mock
    private StructuredOutputParser mockOutputParser;
    @Mock
    private Sessions mockSessions;
    @Mock
    private Explain mockExplain;
    @Mock
    private Thought mockFocusThought;
    @Mock
    private ThoughtContent mockThoughtContent;
    @Mock
    private Thought mockResultThought;
    @Mock
    private Thought mockTraceThought;
    @Mock
    private Thought mockSimilarThought;
    @Mock
    private ThoughtMeta mockThoughtMeta;


    private Cognition cognitiveProcessor;

    @BeforeEach
    void setUp() {
        cognitiveProcessor = new Langchain4JCognition(
                mockChatModel,
                mockMemory,
                mockPromptBuilder,
                mockOutputParser,
                mockSessions,
                mockExplain
        );
    }

    @Test
    void processShouldOrchestrateCallsToCollaborators() {
        // Arrange
        String focusThoughtId = "focus-thought-id";
        List<Double> focusThoughtEmbedding = List.of(1.0, 0.0, 0.0);
        String expectedPrompt = "This is a test prompt.";
        String expectedResponseText = "This is the LLM response.";
        List<Thought> expectedThoughts = List.of(mockResultThought);
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(expectedResponseText));

        // Setup context lists, including the focus thought itself to test the removal logic
        List<Thought> traceContext = new ArrayList<>();
        traceContext.add(mockTraceThought);
        traceContext.add(mockFocusThought); // Add focus thought to context

        List<Thought> similarContext = new ArrayList<>();
        similarContext.add(mockSimilarThought);

        // Stubbing the focus thought
        when(mockFocusThought.id()).thenReturn(focusThoughtId);
        when(mockFocusThought.content()).thenReturn(mockThoughtContent);
        when(mockFocusThought.metadata()).thenReturn(mockThoughtMeta);
        when(mockThoughtMeta.type()).thenReturn(com.senars.core.ThoughtType.GOAL); // Regular thought type
        when(mockThoughtContent.embedding()).thenReturn(focusThoughtEmbedding);

        // Stubbing the memory nexus
        when(mockMemory.getTrace(focusThoughtId)).thenReturn(traceContext);
        when(mockMemory.retrieveSimilar(focusThoughtEmbedding, 5)).thenReturn(similarContext);

        // Stubbing the prompt builder and output parser
        ArgumentCaptor<List<Thought>> contextCaptor = ArgumentCaptor.forClass(List.class);
        when(mockPromptBuilder.build(isNull(), eq(mockFocusThought), contextCaptor.capture())).thenReturn(expectedPrompt);
        when(mockChatModel.generate(ArgumentMatchers.<UserMessage>any())).thenReturn(mockResponse);
        when(mockOutputParser.parse(expectedResponseText)).thenReturn(expectedThoughts);

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert
        assertNotNull(result);
        assertEquals(expectedThoughts, result);

        // Verify that context was assembled correctly (including removing the focus thought)
        List<Thought> capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(new HashSet<>(List.of(mockTraceThought, mockSimilarThought)), new HashSet<>(capturedContext));


        // Verify that the collaborators were called in the correct order with the correct parameters
        verify(mockMemory).getTrace(focusThoughtId);
        verify(mockMemory).retrieveSimilar(focusThoughtEmbedding, 5);
        verify(mockPromptBuilder).build(isNull(), eq(mockFocusThought), anyList());
        verify(mockChatModel).generate(ArgumentMatchers.<UserMessage>any());
        verify(mockOutputParser).parse(expectedResponseText);
    }
}
