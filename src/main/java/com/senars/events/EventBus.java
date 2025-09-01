package com.senars.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple, in-process event bus for decoupling components.
 * This implementation is thread-safe and uses an executor for asynchronous event dispatching.
 */
public class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventBus.class);
    private final Map<Class<? extends Event>, List<EventSubscriber>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Subscribes a listener to a specific type of event.
     * @param eventType The class of the event to listen for.
     * @param subscriber The subscriber that will handle the event.
     * @param <T> The type of the event.
     */
    public <T extends Event> void subscribe(Class<T> eventType, EventSubscriber<T> subscriber) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(subscriber);
        LOGGER.info("New subscriber {} for event type {}", subscriber.getClass().getSimpleName(), eventType.getSimpleName());
    }

    /**
     * Publishes an event, notifying all relevant subscribers asynchronously.
     * @param event The event to publish.
     */
    public void publish(Event event) {
        List<EventSubscriber> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            executor.submit(() -> {
                for (EventSubscriber subscriber : eventSubscribers) {
                    try {
                        subscriber.onEvent(event);
                    } catch (Exception e) {
                        LOGGER.error("Error in event subscriber while handling event {}", event.getClass().getSimpleName(), e);
                    }
                }
            });
        }
    }

    /**
     * Shuts down the event bus executor. Should be called on application exit.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
