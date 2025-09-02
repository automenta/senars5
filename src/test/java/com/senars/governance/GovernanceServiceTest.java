package com.senars.governance;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GovernanceServiceTest {

    @Test
    void testReviewPlanWithBlockedKeyword() {
        // Arrange
        Thought actionPlan = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("This action deletes system files", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
        
        GovernanceService governanceService = new GovernanceService(null);
        
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
        
        GovernanceService governanceService = new GovernanceService(null);
        
        // Act
        Optional<String> result = governanceService.reviewPlan(belief);
        
        // Assert
        assertFalse(result.isPresent());
    }
}