package com.senars.explain;

import com.senars.core.*;
import com.senars.events.EventBus;
import com.senars.events.EventSubscriber;
import com.senars.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An event subscriber that listens for schema optimization events and
 * generates a new goal to create a human-readable report explaining the change.
 */
public class ExplanationGenerator implements EventSubscriber<Events.SchemaOptimizedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExplanationGenerator.class);
    private static final double GOAL_SALIENCE = 80.0; // High, but not critical

    private final EventBus eventBus;

    public ExplanationGenerator(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onEvent(Events.SchemaOptimizedEvent event) {
        LOGGER.info("Received SchemaOptimizedEvent for old schema: {}. Generating XAI report goal.", event.oldSchemaId());

        String goalText = String.format(
                "The schema %s was recently replaced by a new version, %s, due to an automated optimization process. " +
                        "Generate a human-readable report explaining why this change was made. Use the causal links of the new schema to find the optimization goal and the original schema.",
                event.oldSchemaId(),
                event.newSchemaId()
        );

        ThoughtContent content = new ThoughtContent(goalText, null, null, null, null, null, null);

        // The causal links should include the new schema so the XAI system knows where to start looking.
        ThoughtMeta meta = new ThoughtMeta(
                ThoughtType.GOAL,
                ThoughtOrigin.SYSTEM,
                List.of(event.newSchemaId()),
                Instant.now()
        );

        ThoughtState state = new ThoughtState(1.0, GOAL_SALIENCE, 1.0);

        Thought xaiGoal = new Thought(UUID.randomUUID().toString(), content, state, meta);

        LOGGER.info("Generated new XAI report goal: {}", xaiGoal.id());
        eventBus.publish(new Events.NewThoughtCreatedEvent(xaiGoal));
    }
}
