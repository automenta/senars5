package com.senars.lm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.db.DatabaseManager;
import com.senars.events.EventBus;
import com.senars.systems.Memory;
import com.senars.systems.immemory.InMemoryMemory;
import com.senars.explain.Explain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LLMPlanningIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @TempDir
    Path tempDir;
    private Memory memory;
    private ChatLanguageModel chatModel;
    private DatabaseManager dbManager;

    @BeforeEach
    void setUp() throws IOException {
        Path dbFile = tempDir.resolve("test-planning.db");
        dbManager = new DatabaseManager(dbFile);
        memory = new InMemoryMemory(AppConfig.getInstance(), dbManager);
        chatModel = mock(ChatLanguageModel.class);
        Explain explain = mock(Explain.class);
        ToolKit toolKit = mock(ToolKit.class);

        // Load schemas into memory
        loadSchema("planning-schema.json");
        loadSchema("parsing-schema.json");
    }

    private void loadSchema(String schemaName) throws IOException {
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaName)) {
            assertNotNull(schemaStream, schemaName + " not found in resources");
            List<Thought> schemas = objectMapper.readValue(schemaStream, new TypeReference<>() {
            });
            for (Thought schema : schemas) {
                memory.saveThought(schema);
            }
        }
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    }
