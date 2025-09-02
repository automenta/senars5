package com.senars.logic.mdr;

import com.senars.core.*;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class MonitorConfigTest {

    @Test
    void constructor_createsMonitorConfigWithCorrectValues() {
        // Arrange
        String name = "TestMonitor";
        Predicate<Feedback> condition = feedback -> feedback.status() == ActionStatus.FAILURE;
        String description = "Test monitor for failures";

        // Act
        MonitorConfig monitorConfig = new MonitorConfig(name, condition, description);

        // Assert
        assertEquals(name, monitorConfig.getName());
        assertEquals(condition, monitorConfig.getTriggerCondition());
        assertEquals(description, monitorConfig.getDescription());
    }

    @Test
    void toString_returnsFormattedString() {
        // Arrange
        String name = "TestMonitor";
        Predicate<Feedback> condition = feedback -> feedback.status() == ActionStatus.FAILURE;
        String description = "Test monitor for failures";
        MonitorConfig monitorConfig = new MonitorConfig(name, condition, description);

        // Act
        String result = monitorConfig.toString();

        // Assert
        assertTrue(result.contains(name));
        assertTrue(result.contains(description));
    }
}