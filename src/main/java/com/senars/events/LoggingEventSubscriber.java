package com.senars.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple subscriber that logs all events it receives to the console.
 * This provides a real-time narrative of the system's internal state.
 */
public class LoggingEventSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger("EventMonitor");

    /**
     * Subscribes to all relevant event types on the given event bus.
     * @param eventBus The event bus to subscribe to.
     */
    public void subscribeToAll(EventBus eventBus) {
        eventBus.subscribe(Events.NewThoughtCreatedEvent.class, event ->
                LOGGER.info("[NEW_THOUGHT] Type: {}, Origin: {}, ID: {}, Content: '{}'",
                        event.thought().metadata().type(),
                        event.thought().metadata().origin(),
                        event.thought().id(),
                        event.thought().content().text()));

        eventBus.subscribe(Events.FocusThoughtSelectedEvent.class, event ->
                LOGGER.info("[FOCUS] ID: {}, Salience: {}, Clarity: {}, Content: '{}'",
                        event.thought().id(),
                        String.format("%.2f", event.thought().state().salience()),
                        String.format("%.2f", event.thought().state().clarity()),
                        event.thought().content().text()));

        eventBus.subscribe(Events.ActionPlanApprovedEvent.class, event ->
                LOGGER.info("[ACTION_APPROVED] ID: {}, Content: '{}'", event.actionPlan().id(), event.actionPlan().content().text()));

        eventBus.subscribe(Events.ActionPlanVetoedEvent.class, event ->
                LOGGER.warn("[ACTION_VETOED] ID: {}, Reason: {}", event.actionPlan().id(), event.reason()));

        eventBus.subscribe(Events.ClarityUpdatedEvent.class, event ->
                LOGGER.info("[CLARITY_UPDATE] ID: {}, From: {} -> To: {}",
                        event.thoughtId(),
                        String.format("%.2f", event.oldClarity()),
                        String.format("%.2f", event.newClarity())));

        eventBus.subscribe(Events.SchemaOptimizationGoalCreatedEvent.class, event ->
                LOGGER.warn("[SELF_IMPROVE] Goal created to rewrite schema. Reason: {}", event.goal().content().text()));
    }
}
