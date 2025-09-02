package com.senars.logic.mdr;

import com.senars.core.*;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaOptimizationMonitorConfigTest {

    @Mock
    private Memory memory;
    
    private SchemaOptimizationMonitorConfig schemaOptimizationMonitor;
    
    @BeforeEach
    void setUp() {
        schemaOptimizationMonitor = new SchemaOptimizationMonitorConfig(memory);
    }
    
    @Test
    void constructor_setsCorrectProperties() {
        assertEquals("SchemaOptimizationMonitor", schemaOptimizationMonitor.getName());
        assertEquals("Monitors for failed action executions and traces them back to schemas for optimization", 
                schemaOptimizationMonitor.getDescription());
    }
    
    @Test
    void getTriggerCondition_returnsFailurePredicate() {
        // Test with a success feedback
        Thought actionPlan = createActionPlan();
        Feedback successFeedback = new Feedback(ActionStatus.SUCCESS, "test.tool", "Success", 100L, actionPlan);
        assertFalse(schemaOptimizationMonitor.getTriggerCondition().test(successFeedback));
        
        // Test with a failure feedback
        Feedback failureFeedback = new Feedback(ActionStatus.FAILURE, "test.tool", "Failed", 100L, actionPlan);
        assertTrue(schemaOptimizationMonitor.getTriggerCondition().test(failureFeedback));
    }
    
    @Test
    void isSchemaRelatedFailure_withNullActionPlan_returnsFalse() {
        // Arrange
        Feedback feedback = new Feedback(ActionStatus.FAILURE, "test.tool", "Failed", 100L, null);
        
        // Act
        boolean result = schemaOptimizationMonitor.isSchemaRelatedFailure(feedback);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void isSchemaRelatedFailure_withSchemaInTrace_returnsTrue() {
        // Arrange
        String schemaId = "schema-1";
        Thought schema = createSchema(schemaId);
        Thought actionPlan = createActionPlanWithTrace(schemaId);
        Feedback feedback = new Feedback(ActionStatus.FAILURE, "test.tool", "Failed", 100L, actionPlan);
        
        when(memory.getThoughtById(schemaId)).thenReturn(Optional.of(schema));
        
        // Act
        boolean result = schemaOptimizationMonitor.isSchemaRelatedFailure(feedback);
        
        // Assert
        assertTrue(result);
        verify(memory).getThoughtById(schemaId);
    }
    
    @Test
    void isSchemaRelatedFailure_withoutSchemaInTrace_returnsFalse() {
        // Arrange
        String nonSchemaId = "belief-1";
        Thought belief = createBelief(nonSchemaId);
        Thought actionPlan = createActionPlanWithTrace(nonSchemaId);
        Feedback feedback = new Feedback(ActionStatus.FAILURE, "test.tool", "Failed", 100L, actionPlan);
        
        when(memory.getThoughtById(nonSchemaId)).thenReturn(Optional.of(belief));
        
        // Act
        boolean result = schemaOptimizationMonitor.isSchemaRelatedFailure(feedback);
        
        // Assert
        assertFalse(result);
        verify(memory).getThoughtById(nonSchemaId);
    }
    
    private Thought createActionPlan() {
        return new Thought(
                "action-plan-1",
                new ThoughtContent("Test action plan", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.UCR_FORWARD, List.of(), Instant.now())
        );
    }
    
    private Thought createActionPlanWithTrace(String traceId) {
        return new Thought(
                "action-plan-1",
                new ThoughtContent("Test action plan", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.UCR_FORWARD, List.of(traceId), Instant.now())
        );
    }
    
    private Thought createSchema(String id) {
        return new Thought(
                id,
                new ThoughtContent("Test schema", "test-schema", null, null, "Test procedure", null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.SCHEMA, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }
    
    private Thought createBelief(String id) {
        return new Thought(
                id,
                new ThoughtContent("Test belief", null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.SYSTEM, List.of(), Instant.now())
        );
    }
}