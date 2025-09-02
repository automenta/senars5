package com.senars.lm;

import com.google.gson.Gson;
import com.senars.core.Thought;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Responsible for constructing the final, structured prompt to be sent to the LLM.
 * It combines schemas, context, and the focus thought into a coherent prompt.
 */
public class PromptBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptBuilder.class);
    private final Gson gson = new Gson();

    /**
     * Builds a prompt for the LLM.
     *
     * @param schema             The schema thought to use as a template. Can be null.
     * @param focusThought       The main thought being processed.
     * @param context            A list of related thoughts for context.
     * @param toolSpecifications A list of available tools for the LLM.
     * @return A string representing the fully constructed prompt.
     */
    public String build(Thought schema, Thought focusThought, List<Thought> context, List<ToolSpecification> toolSpecifications) {
        StringBuilder prompt = new StringBuilder();

        // 1. Add Schema instructions or a fallback
        if (schema != null && schema.content() != null && schema.content().text() != null) {
            LOGGER.debug("Using schema to build prompt. Schema ID: {}", schema.id());
            prompt.append(schema.content().text());
        } else {
            LOGGER.warn("Schema is null or has no text content. Falling back to simple prompt generation.");
            prompt.append("You are a helpful reasoning engine. Your goal is to decide the next best step.");
        }
        prompt.append("\n\n");

        // 2. Add Tool instructions
        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            prompt.append("You have the following tools available to you:\n");
            prompt.append(gson.toJson(toolSpecifications));
            prompt.append("\nTo use a tool, respond with a JSON object matching the tool's schema.\n");
            prompt.append("If you do not need to use a tool, respond with your final answer or next thought in the structured format expected.\n\n");
        }

        // 3. Add Context
        if (context != null && !context.isEmpty()) {
            prompt.append("Here is some context from previous thoughts:\n");
            for (Thought thought : context) {
                if (thought.content() != null && thought.content().text() != null) {
                    prompt.append("- [").append(thought.metadata().type()).append("] ").append(thought.content().text()).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 4. Add the Focus Thought
        prompt.append("The current focus is a ").append(focusThought.metadata().type()).append(" with the content: '")
                .append(focusThought.content().text()).append("'.\n");
        prompt.append("What is the next logical step or action?");

        return prompt.toString();
    }

    /**
     * Builds a prompt specifically for the explanation task.
     *
     * @param formattedTrace A string containing the formatted reasoning trace.
     * @return A prompt for the LLM to generate a narrative explanation.
     */
    public String buildExplanationPrompt(String formattedTrace) {
        return "You are an AI assistant tasked with explaining your own reasoning. " +
                "Based on the following chain of thoughts, please provide a brief, easy-to-understand narrative " +
                "explaining the reasoning process. Start from the initial goal or belief and walk through how it led to the conclusion.\n\n" +
                "Reasoning Trace:\n" +
                "----------------\n" +
                formattedTrace +
                "----------------\n\n" +
                "Narrative Explanation:";
    }

    public String buildSchemaRewritePrompt(Thought faultySchema) {
        String originalPrompt = String.valueOf(faultySchema.content().procedural());
        // A more advanced version could include examples of failed outputs.
        return "You are a prompt engineering expert. The following prompt (which is a 'schema' for an AI) is underperforming, " +
                "leading to low-quality or inaccurate outputs. " +
                "Your task is to analyze it and rewrite it to be more robust, clear, and effective. " +
                "Return ONLY the rewritten prompt, without any explanation, preamble, or markdown formatting.\n\n" +
                "ORIGINAL PROMPT:\n---\n" +
                originalPrompt + "\n---\n" +
                "REWRITTEN PROMPT:";
    }

    public String buildEffortModelRewritePrompt(String analysis) {
        return "You are a data scientist AI. You are tasked with improving a simple linear effort prediction model.\n" +
                "The model is: effort = (coefficient * textLength) + intercept.\n" +
                "Based on the following analysis of the model's recent performance, provide new values for 'coefficient' and 'intercept'.\n" +
                "Return ONLY the two numbers, separated by a comma. Example: 0.05,1.5\n\n" +
                "PERFORMANCE ANALYSIS:\n---\n" +
                analysis + "\n---\n" +
                "NEW PARAMETERS (coefficient, intercept):";
    }
}
