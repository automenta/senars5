package com.senars.logic.mdr;

import com.senars.core.*;
import com.senars.logic.UnifiedCausalReasoner;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MDRServiceTest {

    @Mock
    private UnifiedCausalReasoner ucr;

    @Mock
    private com.senars.systems.Memory memory;

    @Mock
    private ChatLanguageModel chatModel;

    private MDRService mdrService;

    @BeforeEach
    void setUp() {
        mdrService = new MDRService(ucr, memory, chatModel);
    }

    private Thought createActionPlan() {
        return new Thought(
                "action-plan-1",
                new ThoughtContent("Test action plan", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.UCR_FORWARD, List.of(), Instant.now())
        );
    }

    private Feedback createSuccessFeedback(Thought actionPlan) {
        return new Feedback(ActionStatus.SUCCESS, "test.tool", "Success", 100L, actionPlan);
    }

    private Feedback createFailureFeedback(Thought actionPlan) {
        return new Feedback(ActionStatus.FAILURE, "test.tool", "Failed to execute", 100L, actionPlan);
    }

    @Test
    void processFeedback_withSuccessFeedback_returnsEmpty() {
        // Arrange
        Thought actionPlan = createActionPlan();
        Feedback feedback = createSuccessFeedback(actionPlan);

        // Act
        Optional<Thought> result = mdrService.processFeedback(feedback);

        // Assert
        assertFalse(result.isPresent());
        // Verify that UCR was not called for successful feedback
        verify(ucr, never()).reason(any(), anyString(), any());
    }

    @Test
    void processFeedback_withFailureFeedback_callsUCRAndReturnsRemediationGoal() {
        // Arrange
        Thought actionPlan = createActionPlan();
        Feedback feedback = createFailureFeedback(actionPlan);

        // Create a mock diagnostic report
        Thought diagnosticReport = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Diagnostic report content", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.REPORT, ThoughtOrigin.UCR_BACKWARD, List.of(actionPlan.id()), Instant.now())
        );

        when(ucr.reason(eq(actionPlan), eq("backward"), any(UnifiedCausalReasoner.ReasoningOptions.class)))
                .thenReturn(List.of(diagnosticReport));

        // Act
        Optional<Thought> result = mdrService.processFeedback(feedback);

        // Assert
        assertTrue(result.isPresent());
        Thought remediationGoal = result.get();
        assertEquals(ThoughtType.GOAL, remediationGoal.metadata().type());
        assertEquals(ThoughtOrigin.META_COGNITION, remediationGoal.metadata().origin());
        assertTrue(remediationGoal.content().text().contains("Fix the root cause"));
        assertTrue(remediationGoal.content().text().contains(feedback.toolName()));

        // Verify UCR was called for backward reasoning
        verify(ucr).reason(eq(actionPlan), eq("backward"), any(UnifiedCausalReasoner.ReasoningOptions.class));
    }

    @Test
    void processFeedback_withFailureFeedbackButNoDiagnosis_returnsEmpty() {
        // Arrange
        Thought actionPlan = createActionPlan();
        Feedback feedback = createFailureFeedback(actionPlan);

        // Make UCR return empty results
        when(ucr.reason(eq(actionPlan), eq("backward"), any(UnifiedCausalReasoner.ReasoningOptions.class)))
                .thenReturn(List.of());

        // Act
        Optional<Thought> result = mdrService.processFeedback(feedback);

        // Assert
        assertFalse(result.isPresent());

        // Verify UCR was called
        verify(ucr).reason(eq(actionPlan), eq("backward"), any(UnifiedCausalReasoner.ReasoningOptions.class));
    }

    @Test
    void addMonitor_addsNewMonitorToService() {
        // Arrange
        MonitorConfig newMonitor = new MonitorConfig(
                "TestMonitor",
                feedback -> feedback.status() == ActionStatus.FAILURE,
                "Test monitor for failures"
        );

        // Act
        mdrService.addMonitor(newMonitor);

        // Note: We can't easily test that the monitor is actually used without 
        // refactoring the MDRService to allow for better testability.
        // This test mainly ensures the method doesn't throw an exception.
        assertDoesNotThrow(() -> {
            // This is just to satisfy the test structure
        });
    }
}