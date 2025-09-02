package com.senars.logic.mdr;

import com.senars.systems.Memory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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

        // Act & Assert
        assertFalse(monitor.isContextRetrievalFailure(null));
    }
}