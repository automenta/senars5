package com.senars.cycle;

import com.senars.core.Thought;
import java.util.List;

/**
 * Interface for the Cognitive Processor, which uses an LLM engine to perform
 * the core cognitive work of the system (e.g., inference, planning, reflection).
 */
public interface ICognitiveProcessor {

    /**
     * Processes the single focus Thought selected by the Attention Funnel.
     * This involves assembling context, selecting a schema, calling an LLM,
     * and parsing the output into new Thoughts.
     *
     * @param focusThought The Thought to be processed.
     * @return A list of new Thoughts generated as a result of the processing.
     *         This can include BELIEFs, GOALs, ACTION_PLANs, etc.
     */
    List<Thought> process(Thought focusThought);
}
