package com.senars.lm;

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
    private Explain mockExplain;
    @Mock
    private ToolKit mockToolKit;
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
    @Mock
    private com.senars.core.ThoughtState mockThoughtState;


    private Cognition cognitiveProcessor;

    @BeforeEach
    void setUp() {
        cognitiveProcessor = new Langchain4JCognition(
                mockChatModel,
                mockMemory,
                mockPromptBuilder,
                mockOutputParser,
                mockExplain,
                mockToolKit
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
        when(mockSchemaThought.state()).thenReturn(mockThoughtState);
        when(mockSchemaThought.content()).thenReturn(mock(ThoughtContent.class));

        when(mockMemory.retrieveSimilar(embedding, 5)).thenReturn(new ArrayList<>());
        when(mockMemory.retrieveSimilar(embedding, 5, com.senars.core.ThoughtType.SCHEMA))
                .thenReturn(List.of(new com.senars.systems.ScoredThought(mockSchemaThought, 1.0)));
        when(mockPromptBuilder.build(eq(mockSchemaThought), eq(mockFocusThought), anyList(), anyList())).thenReturn("schema_prompt");
        when(mockChatModel.generate(any(UserMessage.class))).thenReturn(Response.from(AiMessage.from("response")));
        when(mockToolKit.parse(anyString())).thenReturn(null);

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert: The result should now be a goal to parse the response.
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(com.senars.core.ThoughtType.GOAL, result.getFirst().metadata().type());
        assertEquals("senars:parse_text", result.getFirst().content().symbolic());
        assertEquals("response", result.getFirst().content().text());
    }

    @Test
    void processShouldUseFallbackWhenNoSchemaFound() {
        // Arrange
        String focusThoughtId = "focus-id";
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        setupFocusThought(focusThoughtId, embedding);

        when(mockMemory.retrieveSimilar(embedding, 5)).thenReturn(new ArrayList<>());
        when(mockMemory.retrieveSimilar(embedding, 5, com.senars.core.ThoughtType.SCHEMA))
                .thenReturn(List.of()); // No schema found
        when(mockPromptBuilder.build(isNull(), eq(mockFocusThought), anyList(), anyList())).thenReturn("fallback_prompt");
        when(mockChatModel.generate(any(UserMessage.class))).thenReturn(Response.from(AiMessage.from("response")));
        when(mockToolKit.parse(anyString())).thenReturn(null);

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert: The result should now be a goal to parse the response.
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(com.senars.core.ThoughtType.GOAL, result.getFirst().metadata().type());
        assertEquals("senars:parse_text", result.getFirst().content().symbolic());
        assertEquals("response", result.getFirst().content().text());
    }


    @Test
    @SuppressWarnings("unchecked")
    void processShouldOrchestrateCallsToCollaborators() {
        // Arrange
        String focusThoughtId = "focus-thought-id";
        List<Double> focusThoughtEmbedding = List.of(1.0, 0.0, 0.0);
        setupFocusThought(focusThoughtId, focusThoughtEmbedding);

        String expectedPrompt = "This is a test prompt.";
        String expectedResponseText = "This is the LLM response.";
        Response<AiMessage> mockResponse = Response.from(AiMessage.from(expectedResponseText));

        // Setup context lists
        List<Thought> traceContext = List.of(mockTraceThought);
        List<com.senars.systems.ScoredThought> similarContext = List.of(new com.senars.systems.ScoredThought(mockSimilarThought, 1.0));

        // Stubbing the memory nexus
        when(mockMemory.getTrace(focusThoughtId)).thenReturn(traceContext);
        when(mockMemory.retrieveSimilar(focusThoughtEmbedding, 5)).thenReturn(similarContext);
        when(mockMemory.retrieveSimilar(focusThoughtEmbedding, 5, com.senars.core.ThoughtType.SCHEMA)).thenReturn(List.of());
        when(mockToolKit.parse(anyString())).thenReturn(null);


        // Stubbing the prompt builder and output parser
        ArgumentCaptor<List<Thought>> contextCaptor = ArgumentCaptor.forClass(List.class);
        when(mockPromptBuilder.build(isNull(), eq(mockFocusThought), contextCaptor.capture(), anyList())).thenReturn(expectedPrompt);
        when(mockChatModel.generate(ArgumentMatchers.<UserMessage>any())).thenReturn(mockResponse);

        // Act
        List<Thought> result = cognitiveProcessor.process(mockFocusThought);

        // Assert
        assertNotNull(result);
        // The process now returns a "parse" goal instead of directly parsing.
        assertEquals(1, result.size());
        Thought parseGoal = result.getFirst();
        assertEquals(com.senars.core.ThoughtType.GOAL, parseGoal.metadata().type());
        assertEquals("senars:parse_text", parseGoal.content().symbolic());
        assertEquals(expectedResponseText, parseGoal.content().text());
        assertEquals(focusThoughtId, parseGoal.metadata().trace().getFirst());


        // Verify that context was assembled correctly
        List<Thought> capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(new HashSet<>(List.of(mockTraceThought, mockSimilarThought)), new HashSet<>(capturedContext));


        // Verify that the collaborators were called in the correct order with the correct parameters
        verify(mockMemory).getTrace(focusThoughtId);
        verify(mockMemory).retrieveSimilar(focusThoughtEmbedding, 5);
        verify(mockPromptBuilder).build(isNull(), eq(mockFocusThought), anyList(), anyList());
        verify(mockChatModel).generate(ArgumentMatchers.<UserMessage>any());
        // Verify outputParser is no longer called directly
        verify(mockOutputParser, never()).parse(anyString());
    }
}
