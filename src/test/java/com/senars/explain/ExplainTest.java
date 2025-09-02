package com.senars.explain;

import com.senars.core.*;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExplainTest {

    @Mock
    private Memory memory;

    private Explain explain;

    @BeforeEach
    void setUp() {
        explain = new Explain(memory);
    }

    private Thought createTestThought(String id, String text, ThoughtType type, List<String> trace) {
        // Create causal links for the trace
        Set<CausalLink> causalLinks = new HashSet<>();
        if (trace != null) {
            for (String traceId : trace) {
                causalLinks.add(new CausalLink(traceId, id, CausalRelationType.DIRECT_CAUSATION));
            }
        }

        return new Thought(
                id,
                new ThoughtContent(text, null, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(type, ThoughtOrigin.USER, trace, causalLinks, Instant.now())
        );
    }

    @Test
    void testGetCausalChain_Success() {
        Thought thought1 = createTestThought("id1", "Goal: A", ThoughtType.GOAL, Collections.emptyList());
        Thought thought2 = createTestThought("id2", "Action: B", ThoughtType.ACTION, List.of("id1"));

        when(memory.getThoughtById("id1")).thenReturn(Optional.of(thought1));
        when(memory.getThoughtById("id2")).thenReturn(Optional.of(thought2));

        List<Thought> trace = explain.getCausalChain(thought2);

        assertNotNull(trace);
        // The causal chain should include both thoughts
        assertEquals(2, trace.size());
        assertTrue(trace.contains(thought1));
        assertTrue(trace.contains(thought2));
    }

    @Test
    void testGetCausalChain_Empty() {
        Thought thought = createTestThought("id1", "Goal: A", ThoughtType.GOAL, Collections.emptyList());

        List<Thought> trace = explain.getCausalChain(thought);

        assertNotNull(trace);
        // The causal chain should include the thought itself even if it has no causal links
        assertEquals(1, trace.size());
        assertEquals(thought, trace.getFirst());
    }

    @Test
    void testFormatCausalChain_Success() {
        Thought thought1 = createTestThought("id1", "Goal: A", ThoughtType.GOAL, Collections.emptyList());
        Thought thought2 = createTestThought("id2", "Action: B", ThoughtType.ACTION, List.of("id1"));
        List<Thought> trace = List.of(thought1);

        String formatted = explain.formatCausalChain(trace, thought2);

        assertNotNull(formatted);
        assertTrue(formatted.contains("Goal: A"));
        assertTrue(formatted.contains("Action: B"));
    }

    @Test
    void testFormatCausalChain_Empty() {
        Thought thought = createTestThought("id1", "Goal: A", ThoughtType.GOAL, Collections.emptyList());
        List<Thought> trace = Collections.emptyList();

        String formatted = explain.formatCausalChain(trace, thought);

        assertNotNull(formatted);
        assertTrue(formatted.contains("no recorded causal chain"));
    }
}
