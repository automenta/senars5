package com.senars.logic;

import com.senars.core.Feedback;
import com.senars.core.Thought;
import com.senars.systems.Memory;
import com.senars.events.EventBus;

import java.util.List;

/**
 * Factory for creating Unified Causal Reasoner instances.
 */
public class UCRFactory {
    
    public static UnifiedCausalReasoner createUCR(Memory memory, EventBus eventBus) {
        return new UnifiedCausalReasonerImpl(memory, eventBus);
    }
}