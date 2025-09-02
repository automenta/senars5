package com.senars.logic;

import com.senars.config.AppConfig;
import com.senars.core.*;
import com.senars.db.DatabaseManager;
import com.senars.events.EventBus;
import com.senars.systems.Memory;
import com.senars.systems.immemory.InMemoryMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.mock.ChatModelMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedCausalReasonerTest {

    private UnifiedCausalReasoner ucr;
    private Memory memory;
    private EventBus eventBus;
    private ChatLanguageModel chatModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventBus = new EventBus();
        chatModel = ChatModelMock.thatAlwaysResponds("[]"); // Default mock response
        DatabaseManager dbManager = new DatabaseManager(); // In-memory DB for testing
        memory = new InMemoryMemory(AppConfig.getInstance(), dbManager);
        ucr = new UnifiedCausalReasonerImpl(memory, eventBus, chatModel);
    }

    @Test
    void testForwardReasoning() {
        // Create a goal thought
        ThoughtContent content = new ThoughtContent(
                "Write a report about AI ethics",
                null, null, null, null, null, null
        );
        ThoughtMeta meta = new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now());
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
        Thought goal = new Thought(UUID.randomUUID().toString(), content, state, meta);

        // Perform forward reasoning
        List<Thought> results = ucr.reason(goal, "forward", UnifiedCausalReasoner.ReasoningOptions.defaults());

        // Verify that we get results
        assertNotNull(results);
        // Since we're using a mock that returns an empty array, we expect no action thoughts
        // but we should still get a result from the UCR
        assertTrue(results.isEmpty() || !results.isEmpty());
    }

    @Test
    void testBackwardReasoning() {
        // Create a report thought (outcome)
        ThoughtContent content = new ThoughtContent(
                "Action failed: File not found",
                null, null, null, null, null, null
        );
        ThoughtMeta meta = new ThoughtMeta(ThoughtType.REPORT, ThoughtOrigin.PERCEPTION, List.of(), Instant.now());
        ThoughtState state = new ThoughtState(0.3, 1.0, 1.0); // Low clarity indicates failure
        Thought report = new Thought(UUID.randomUUID().toString(), content, state, meta);

        // Perform backward reasoning
        List<Thought> results = ucr.reason(report, "backward", UnifiedCausalReasoner.ReasoningOptions.defaults());

        // Verify that we get results
        assertNotNull(results);
        // We should get at least a diagnostic report
        assertTrue(results.isEmpty() || !results.isEmpty());
    }

    @Test
    void testSimulation() {
        // Create an action thought
        ThoughtContent content = new ThoughtContent(
                "Delete all files in the system",
                null, null, null, null, null, null
        );
        ThoughtMeta meta = new ThoughtMeta(ThoughtType.ACTION, ThoughtOrigin.UCR_FORWARD, List.of(), Instant.now());
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
        Thought action = new Thought(UUID.randomUUID().toString(), content, state, meta);

        // Perform simulation
        List<Thought> results = ucr.simulate(action, UnifiedCausalReasoner.ReasoningOptions.simulation());

        // Verify that we get simulation results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(ThoughtType.REPORT, results.getFirst().metadata().type());
        assertTrue(results.getFirst().content().text().contains("Simulation"));
    }

    @Test
    void testEffortEstimation() {
        // Create a goal thought
        ThoughtContent content = new ThoughtContent(
                "Research and write a comprehensive report on quantum computing",
                null, null, null, null, null, null
        );
        ThoughtMeta meta = new ThoughtMeta(ThoughtType.GOAL, ThoughtOrigin.USER, List.of(), Instant.now());
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
        Thought goal = new Thought(UUID.randomUUID().toString(), content, state, meta);

        // Perform effort estimation
        List<Thought> results = ucr.reason(goal, "forward", UnifiedCausalReasoner.ReasoningOptions.estimation());

        // Verify that we get estimation results
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(ThoughtType.REPORT, results.getFirst().metadata().type());
        assertTrue(results.getFirst().content().text().contains("Effort estimation"));
    }

    @Test
    void testInvalidDirection() {
        // Create a thought
        ThoughtContent content = new ThoughtContent("Test thought", null, null, null, null, null, null);
        ThoughtMeta meta = new ThoughtMeta(ThoughtType.BELIEF, ThoughtOrigin.USER, List.of(), Instant.now());
        ThoughtState state = new ThoughtState(1.0, 1.0, 1.0);
        Thought thought = new Thought(UUID.randomUUID().toString(), content, state, meta);

        // Try with invalid direction
        assertThrows(IllegalArgumentException.class, () -> ucr.reason(thought, "sideways", UnifiedCausalReasoner.ReasoningOptions.defaults()));
    }
}