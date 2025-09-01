package com.senars.llm;

import com.senars.core.Sessions;
import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.cycle.Cognition;
import com.senars.cycle.Inference;
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
    private Inference mockInference;
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
    @Mock
    private Thought mockSchemaThought;


    private Cognition cognitiveProcessor;

    @BeforeEach
    void setUp() {
        cognitiveProcessor = new Langchain4JCognition(
                mockChatModel,
                mockMemory,
                mockPromptBuilder,
                mockOutputParser,
                mockSessions,
                mockExplain,
                mockInference
        );
    }

    private void setupFocusThought(String id, List<Double> embedding) {
        when(mockFocusThought.id()).thenReturn(id);
        when(mockFocusThought.content()).thenReturn(mockThoughtContent);
        when(mockFocusThought.metadata()).thenReturn(mockThoughtMeta);
        when(mockThoughtMeta.type()).thenReturn(com.senars.core.ThoughtType.GOAL);
        when(mockThoughtContent.embedding()).thenReturn(embedding);
    }

    @Test
    void processShouldUseSchemaWhenFound() {
        // Arrange
        String focusThoughtId = "focus-id";
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        setupFocusThought(focusThoughtId, embedding);

        when(mockMemory.retrieveSimilar(embedding, 5)).thenReturn(new ArrayList<>());
        when(mockMemory.retrieveSimilar(embedding, 1, com.senars.core.ThoughtType.SCHEMA))
                .thenReturn(List.of(mockSchemaThought));
        when(mockPromptBuilder.build(eq(mockSchemaThought), eq(mockFocusThought), anyList())).thenReturn("schema_prompt");
        when(mockChatModel.generate(any(UserMessage.class))).thenReturn(Response.from(AiMessage.from("response")));
        when(mockOutputParser.parse(anyString())).thenReturn(List.of(mockResultThought));


        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert
        assertNotNull(result);
        assertEquals(List.of(mockResultThought), result);
        verify(mockMemory).retrieveSimilar(embedding, 1, com.senars.core.ThoughtType.SCHEMA);
        verify(mockPromptBuilder).build(eq(mockSchemaThought), eq(mockFocusThought), anyList());
    }

    @Test
    void processShouldUseFallbackWhenNoSchemaFound() {
        // Arrange
        String focusThoughtId = "focus-id";
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        setupFocusThought(focusThoughtId, embedding);

        when(mockMemory.retrieveSimilar(embedding, 5)).thenReturn(new ArrayList<>());
        when(mockMemory.retrieveSimilar(embedding, 1, com.senars.core.ThoughtType.SCHEMA))
                .thenReturn(List.of()); // No schema found
        when(mockPromptBuilder.build(isNull(), eq(mockFocusThought), anyList())).thenReturn("fallback_prompt");
        when(mockChatModel.generate(any(UserMessage.class))).thenReturn(Response.from(AiMessage.from("response")));
        when(mockOutputParser.parse(anyString())).thenReturn(List.of(mockResultThought));

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert
        assertNotNull(result);
        assertEquals(List.of(mockResultThought), result);
        verify(mockMemory).retrieveSimilar(embedding, 1, com.senars.core.ThoughtType.SCHEMA);
        verify(mockPromptBuilder).build(isNull(), eq(mockFocusThought), anyList());
    }


    @Test
    void processShouldOrchestrateCallsToCollaborators() {
        // Arrange
        String focusThoughtId = "focus-thought-id";
        List<Double> focusThoughtEmbedding = List.of(1.0, 0.0, 0.0);
        setupFocusThought(focusThoughtId, focusThoughtEmbedding);

        String expectedPrompt = "This is a test prompt.";
        String expectedResponseText = "This is the LLM response.";
        List<Thought> expectedThoughts = List.of(mockResultThought);
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(expectedResponseText));

        // Setup context lists
        List<Thought> traceContext = List.of(mockTraceThought);
        List<Thought> similarContext = List.of(mockSimilarThought);

        // Stubbing the memory nexus
        when(mockMemory.getTrace(focusThoughtId)).thenReturn(traceContext);
        when(mockMemory.retrieveSimilar(focusThoughtEmbedding, 5)).thenReturn(similarContext);
        when(mockMemory.retrieveSimilar(focusThoughtEmbedding, 1, com.senars.core.ThoughtType.SCHEMA)).thenReturn(List.of());


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

        // Verify that context was assembled correctly
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
