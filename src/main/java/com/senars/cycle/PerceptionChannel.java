package com.senars.cycle;

import com.senars.core.Thought;

import java.util.List;

/**
 * An interface for a single source of perception.
 * Each channel is responsible for a specific input mechanism,
 * such as console input, file monitoring, or web scraping.
 */
public interface PerceptionChannel {

    /**
     * Perceives new information from the specific channel and translates it into a list of Thoughts.
     *
     * @return A list of new Thought objects. The list can be empty if no new information is perceived.
     * @throws ShutdownException if a shutdown command is detected from the perception source.
     */
    List<Thought> perceive() throws ShutdownException;
}
