package com.senars.logic.mdr;

import com.senars.core.Feedback;

import java.util.function.Predicate;

/**
 * Configuration for a Monitor in the Monitor-Diagnose-Remediate pattern.
 * Defines what events to watch for and how to trigger the diagnostic process.
 */
public class MonitorConfig {
    private final String name;
    private final Predicate<Feedback> triggerCondition;
    private final String description;

    public MonitorConfig(String name, Predicate<Feedback> triggerCondition, String description) {
        this.name = name;
        this.triggerCondition = triggerCondition;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public Predicate<Feedback> getTriggerCondition() {
        return triggerCondition;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "MonitorConfig{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}