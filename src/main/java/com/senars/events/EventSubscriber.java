package com.senars.events;

/**
 * An interface for components that wish to subscribe to system events.
 * @param <T> The type of event this subscriber listens to.
 */
@FunctionalInterface
public interface EventSubscriber<T extends Event> {
    void onEvent(T event);
}
