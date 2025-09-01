package com.senars.xai;

import com.senars.core.Thought;
import com.senars.core.ThoughtContent;
import com.senars.core.ThoughtMeta;
import com.senars.core.ThoughtOrigin;
import com.senars.core.ThoughtState;
import com.senars.core.ThoughtType;
import com.senars.systems.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplainTest {

    @Mock
    private Memory memoryNexus;

    private Explain explain;

    @BeforeEach
    void setUp() {
        explain = new Explain(memoryNexus);
    }

    private Thought createTestThought(String id, String text, ThoughtType type, List<String> trace) {
        return new Thought(
                id,
                new ThoughtContent(text, null, null, null, null, null),
                new ThoughtState(1.0, 1.0, 1.0),
                new ThoughtMeta(type, ThoughtOrigin.USER, trace, Instant.now())
        );
    }

    @Test
    void testGetTrace_Success() {
        Thought thought1 = createTestThought("id1", "Goal: A", ThoughtType.GOAL, Collections.emptyList());
        Thought thought2 = createTestThought("id2", "Action: B", ThoughtType.ACTION_PLAN, List.of("id1"));

        when(memoryNexus.getThoughtById("id1")).thenReturn(Optional.of(thought1));

        List<Thought> trace = explain.getTrace(thought2);

        assertNotNull(trace);
        assertEquals(1, trace.size());
        assertEquals("id1", trace.get(0).id());
        verify(memoryNexus, times(1)).getThoughtById("id1");
    }

    @Test
    void testGetTrace_EmptyTrace() {
        Thought thought1 = createTestThought("id1", "Goal: A", ThoughtType.GOAL, Collections.emptyList());
        List<Thought> trace = explain.getTrace(thought1);
        assertTrue(trace.isEmpty());
    }

    @Test
    void testFormatTrace_Success() {
        Thought thought1 = createTestThought("id1", "This is a goal.", ThoughtType.GOAL, Collections.emptyList());
        Thought thought2 = createTestThought("id2", "This is an action.", ThoughtType.ACTION_PLAN, List.of("id1"));

        String formatted = explain.formatTrace(List.of(thought1), thought2);

        assertTrue(formatted.contains("The reasoning for 'This is an action.' was as follows:"));
        assertTrue(formatted.contains("1. [GOAL] This is a goal."));
        assertTrue(formatted.contains("Which led to the final conclusion."));
    }

    @Test
    void testFormatTrace_EmptyTrace() {
        Thought thought1 = createTestThought("id1", "This is a goal.", ThoughtType.GOAL, Collections.emptyList());
        String formatted = explain.formatTrace(Collections.emptyList(), thought1);
        assertEquals("The thought 'This is a goal.' has no recorded reasoning trace. It may be a foundational thought or user input.", formatted);
    }
}
