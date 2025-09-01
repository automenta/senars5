package com.senars.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Handles application configuration by loading properties from the config.properties file.
 */
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "config.properties";

    private final Properties properties;

    private AppConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                LOGGER.error("Sorry, unable to find " + CONFIG_FILE);
                // In a real application, you might want to throw a runtime exception
                // or have a more robust fallback mechanism.
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            LOGGER.error("Error loading configuration file " + CONFIG_FILE, ex);
        }
    }

    public static AppConfig getInstance() {
        return ConfigHolder.INSTANCE;
    }

    public String getLlmApiUrl() {
        return properties.getProperty("llm.api.url", "http://localhost:11434");
    }

    public String getLlmModelName() {
        return properties.getProperty("llm.model.name", "mistral");
    }

    public int getLlmApiTimeout() {
        String timeoutStr = properties.getProperty("llm.api.timeout", "60");
        return Integer.parseInt(timeoutStr);
    }

    public String getVectorStoreFilePath() {
        return properties.getProperty("vectorstore.filepath", "./data/vector_store.json");
    }

    public String getGraphDbFilePath() {
        return properties.getProperty("graphdb.filepath", "./data/graph_db.json");
    }

    // Singleton holder
    private static class ConfigHolder {
        private static final AppConfig INSTANCE = new AppConfig();
    }
}
