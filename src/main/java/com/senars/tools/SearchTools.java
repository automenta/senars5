package com.senars.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tool for performing web searches.
 * Note: This is a dummy implementation for now.
 */
public class SearchTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTools.class);

    @Tool("Searches the web for a given query and returns the most relevant URL.")
    public String search(String query) {
        LOGGER.info("Performing dummy search for query: {}", query);
        // This is a dummy implementation that returns a hardcoded URL.
        // In a real implementation, this would call a search engine API.
        return "https://openai.com/blog/";
    }
}
