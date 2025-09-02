package com.senars.governance;

import com.senars.core.*;
import com.senars.logic.UnifiedCausalReasoner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GovernanceServiceTest {

    @Mock
    private UnifiedCausalReasoner ucr;

    private GovernanceService governanceService;

    @BeforeEach
    void setUp() {
        governanceService = new GovernanceService(ucr);
    }

    @Test
    void testReviewPlanWithBlockedKeyword() {
        // Arrange
        Thought actionPlan = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("This action deletes system files", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );

        // Simulate that the UCR does not find any issues
        when(ucr.simulate(any(), any())).thenReturn(List.of());

        // Act
        Optional<String> result = governanceService.reviewPlan(actionPlan);

        // Assert
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("delete system"));
    }

    @Test
    void testReviewPlanWithNonActionThought() {
        // Arrange
        Thought belief = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("This is a belief", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );

        // Act
        Optional<String> result = governanceService.reviewPlan(belief);

        // Assert
        assertFalse(result.isPresent());
    }
}