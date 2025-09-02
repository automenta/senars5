package com.senars.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiTool.class);

    @Tool("Performs an HTTP GET request to the given URL and returns the response body as a string. Useful for interacting with JSON APIs.")
    public String fetchGetRequest(String url) {
        LOGGER.info("Attempting to fetch URL: {}", url);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Accept", "application/json") // Assume JSON response
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.info("Successfully fetched URL: {}. Status code: {}", url, response.statusCode());
                return response.body();
            } else {
                String errorMsg = String.format("Error: Received status code %d from %s. Response body: %s", response.statusCode(), url, response.body());
                LOGGER.error(errorMsg);
                return errorMsg;
            }

        } catch (URISyntaxException e) {
            String errorMsg = "Error: The provided URL is invalid: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            return errorMsg;
        } catch (IOException | InterruptedException e) {
            String errorMsg = "Error: Failed to send request or was interrupted: " + e.getMessage();
            LOGGER.error(errorMsg, e);
            return errorMsg;
        }
    }
}
