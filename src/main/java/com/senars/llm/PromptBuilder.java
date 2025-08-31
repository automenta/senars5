package com.senars.llm;

import com.senars.core.Thought;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Responsible for constructing the final, structured prompt to be sent to the LLM.
 * It combines schemas, context, and the focus thought into a coherent prompt.
 */
public class PromptBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptBuilder.class);

    /**
     * Builds a prompt for the LLM.
     *
     * @param schema        The schema thought to use as a template. Can be null.
     * @param focusThought  The main thought being processed.
     * @param context       A list of related thoughts for context.
     * @return A string representing the fully constructed prompt.
     */
    public String build(Thought schema, Thought focusThought, List<Thought> context) {
        if (schema != null && schema.content() != null && schema.content().text() != null) {
            LOGGER.debug("Using schema to build prompt. Schema ID: {}", schema.id());
            String template = schema.content().text();

            String focusText = (focusThought.content() != null && focusThought.content().text() != null)
                    ? focusThought.content().text() : "";
            template = template.replace("{{focus}}", focusText);

            StringBuilder contextBuilder = new StringBuilder();
            for (Thought thought : context) {
                if (thought.content() != null && thought.content().text() != null) {
                    contextBuilder.append("- ").append(thought.content().text()).append("\n");
                }
            }
            template = template.replace("{{context}}", contextBuilder.toString());

            return template;
        } else {
            LOGGER.warn("Schema is null or has no text content. Falling back to simple prompt generation.");
            return "Based on the following thought, what should be the next step? Thought: " + focusThought.content().text();
        }
    }
}
