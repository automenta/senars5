package com.senars.attention;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.logic.UnifiedCausalReasoner;
import com.senars.salience.SalienceCalculator;
import com.senars.systems.Memory;
import com.senars.motive.MotiveHierarchy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttentionServiceTest {

    @Test
    void testSelectFocusThoughtWithEmptyList() {
        // Arrange
        Memory memory = mock(Memory.class);
        SalienceCalculator salienceCalculator = mock(SalienceCalculator.class);
        UnifiedCausalReasoner ucr = mock(UnifiedCausalReasoner.class);
        MotiveHierarchy motiveHierarchy = mock(MotiveHierarchy.class);
        
        AttentionService attentionService = new AttentionService(memory, salienceCalculator, ucr, motiveHierarchy);
        
        // Act
        Optional<Thought> result = attentionService.selectFocusThought(List.of());
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void testSelectFocusThoughtWithSingleCandidate() {
        // Arrange
        Memory memory = mock(Memory.class);
        SalienceCalculator salienceCalculator = mock(SalienceCalculator.class);
        UnifiedCausalReasoner ucr = mock(UnifiedCausalReasoner.class);
        MotiveHierarchy motiveHierarchy = mock(MotiveHierarchy.class);
        
        Thought thought = new Thought(
                UUID.randomUUID().toString(),
                new ThoughtContent("Test thought", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
        
        when(salienceCalculator.calculate(thought, motiveHierarchy)).thenReturn(0.5);
        
        AttentionService attentionService = new AttentionService(memory, salienceCalculator, ucr, motiveHierarchy);
        
        // Act
        Optional<Thought> result = attentionService.selectFocusThought(List.of(thought));
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(thought.id(), result.get().id());
    }
}