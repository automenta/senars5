package com.senars.tools;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.events.EventBus;
import com.senars.events.Events;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A tool for managing the lifecycle of schemas within the cognitive architecture.
 * This includes rewriting schemas, deprecating old ones, and managing their state.
 */
public class SchemaManagementTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaManagementTool.class);
    private static final double DEPRECATED_SCHEMA_CLARITY = 0.1;
    private static final double NEW_SCHEMA_INITIAL_CLARITY = 0.75;

    private final Memory memory;
    private final LogicEngine logicEngine;
    private final EventBus eventBus;

    public SchemaManagementTool(Memory memory, LogicEngine logicEngine, EventBus eventBus) {
        this.memory = memory;
        this.logicEngine = logicEngine;
        this.eventBus = eventBus;
    }

    @Tool("Rewrites an existing schema with new procedural content and deprecates the original. Use this to optimize or fix a broken schema.")
    public String rewriteSchema(
            String oldSchemaId,
            String newSchemaName,
            String newSchemaProceduralContent
    ) {
        LOGGER.info("Executing rewriteSchema tool for old schema ID: {}", oldSchemaId);

        // 1. Retrieve the old schema to serve as a template
        Thought oldSchema = memory.getThoughtById(oldSchemaId)
                .orElseThrow(() -> new IllegalArgumentException("Schema with ID " + oldSchemaId + " not found."));

        if (oldSchema.metadata().type() != ThoughtType.SCHEMA) {
            throw new IllegalArgumentException("Thought with ID " + oldSchemaId + " is not a Schema.");
        }

        // 2. Create the new schema
        Thought newSchema = createNewSchemaFromOld(oldSchema, newSchemaName, newSchemaProceduralContent);
        memory.saveThought(newSchema);
        LOGGER.info("Created new schema {} as a replacement for {}", newSchema.id(), oldSchemaId);

        // 3. Deprecate the old schema
        deprecateSchema(oldSchema);
        LOGGER.info("Deprecated old schema {}", oldSchemaId);

        // 4. Publish an event to notify the system of the change
        eventBus.publish(new Events.SchemaOptimizedEvent(oldSchema.id(), newSchema.id()));

        return "Successfully created new schema " + newSchema.id() + " and deprecated old schema " + oldSchemaId + ".";
    }

    private Thought createNewSchemaFromOld(Thought oldSchema, String newSchemaName, String newProceduralContent) {
        ThoughtContent newContent = new ThoughtContent(
                newSchemaName,
                oldSchema.content().symbolic(), // Inherit symbolic representation
                null,
                null,
                newProceduralContent, // The new, improved prompt
                null,
                oldSchema.content().rules() // Inherit rules if they exist
        );

        ThoughtMeta newMeta = new ThoughtMeta(
                ThoughtType.SCHEMA,
                ThoughtOrigin.LLM_INFERENCE, // It was created by an LM-driven optimization process
                List.of(oldSchema.id()), // Trace back to the original schema
                Instant.now()
        );

        ThoughtState newState = new ThoughtState(
                NEW_SCHEMA_INITIAL_CLARITY,
                oldSchema.state().salience(), // Start with similar salience
                1.0 // High activation
        );

        return new Thought(UUID.randomUUID().toString(), newContent, newState, newMeta);
    }

    private void deprecateSchema(Thought oldSchema) {
        // Reduce clarity to make it less likely to be selected
        ThoughtState deprecatedState = new ThoughtState(
                DEPRECATED_SCHEMA_CLARITY,
                oldSchema.state().salience(),
                oldSchema.state().activation()
        );
        Thought deprecatedSchema = new Thought(
                oldSchema.id(),
                oldSchema.content(),
                deprecatedState,
                oldSchema.metadata()
        );
        memory.saveThought(deprecatedSchema);

        // Assert a fact into the logic engine for the governor to use
        String fact = String.format("is_deprecated('%s').", oldSchema.id());
        logicEngine.assertFact(fact);
        LOGGER.debug("Asserted logical fact to deprecate schema: {}", fact);
    }
}
