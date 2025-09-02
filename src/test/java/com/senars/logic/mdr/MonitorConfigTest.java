package com.senars.logic.mdr;

import com.senars.core.ActionStatus;
import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.systems.Memory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MotiveRefinementMonitorConfigTest {

    @Test
    void testMotiveRefinementMonitorCreation() {
        // Arrange
        Memory memory = mock(Memory.class);

        // Act
        MotiveRefinementMonitorConfig monitor = new MotiveRefinementMonitorConfig(memory);

        // Assert
        assertNotNull(monitor);
        assertEquals("MotiveRefinementMonitor", monitor.getName());
        assertNotNull(monitor.getTriggerCondition());
        assertNotNull(monitor.getDescription());
    }
}

class MemoryCurationMonitorConfigTest {

    @Test
    void testMemoryCurationMonitorCreation() {
        // Arrange
        Memory memory = mock(Memory.class);

        // Act
        MemoryCurationMonitorConfig monitor = new MemoryCurationMonitorConfig(memory);

        // Assert
        assertNotNull(monitor);
        assertEquals("MemoryCurationMonitor", monitor.getName());
        assertNotNull(monitor.getTriggerCondition());
        assertNotNull(monitor.getDescription());
    }

    @Test
    void testIsContextRetrievalFailure() {
        // Arrange
        Memory memory = mock(Memory.class);
        MemoryCurationMonitorConfig monitor = new MemoryCurationMonitorConfig(memory);
        Feedback feedback = mock(Feedback.class);
        when(feedback.output()).thenReturn("some output context retrieval failure more output");
        when(feedback.status()).thenReturn(ActionStatus.FAILURE);

        // Act & Assert
        assertFalse(monitor.isContextRetrievalFailure(null));
        assertTrue(monitor.isContextRetrievalFailure(feedback));
    }
}