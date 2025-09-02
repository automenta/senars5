package com.senars.logic;

import com.senars.events.EventBus;
import com.senars.systems.Memory;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Factory for creating Unified Causal Reasoner instances.
 */
public class UCRFactory {

    public static UnifiedCausalReasoner createUCR(Memory memory, EventBus eventBus, ChatLanguageModel chatModel) {
        return new UnifiedCausalReasonerImpl(memory, eventBus, chatModel);
    }
}