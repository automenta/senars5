package com.senars.tools;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * A tool for performing web searches using DuckDuckGo's HTML endpoint.
 * This implementation does not require an API key.
 */
public class SearchTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTools.class);
    private static final String DUCKDUCKGO_SEARCH_URL = "https://html.duckduckgo.com/html/?q=";

    @Tool("Searches the web for a given query and returns a list of result URLs, separated by newlines.")
    public String search(String query) {
        LOGGER.info("Performing web search for query: {}", query);
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(DUCKDUCKGO_SEARCH_URL + encodedQuery)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .get();

            Elements results = doc.select("a.result__a");

            if (results.isEmpty()) {
                return "No search results found.";
            }

            return results.stream()
                    .map(element -> element.attr("href"))
                    .collect(Collectors.joining("\n"));

        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Failed to encode query: {}", query, e);
            return "Error: Failed to encode search query.";
        } catch (IOException e) {
            LOGGER.error("Failed to perform search for query: {}", query, e);
            return "Error: Failed to connect to search engine. " + e.getMessage();
        }
    }
}
