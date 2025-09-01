package com.senars.tools;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A tool for reading content from web pages.
 */
public class WebTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebTools.class);

    @Tool("Reads the textual content of a given URL.")
    public String read(String url) {
        LOGGER.info("Reading content from URL: {}", url);
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.body().text();
        } catch (IOException e) {
            LOGGER.error("Failed to read content from URL: {}", url, e);
            return "Error: " + e.getMessage();
        }
    }
}
