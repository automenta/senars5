package com.senars.systems.immemory;

import com.senars.core.Thought;
import com.senars.cycle.Perception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * A stub implementation of the perception system that does nothing.
 * It's useful for wiring the application when no real perception source is available.
 */
public class StubPerception implements Perception {

    private static final Logger LOGGER = LoggerFactory.getLogger(StubPerception.class);

    @Override
    public List<Thought> perceive() {
        // This stub implementation perceives nothing from the environment.
        return Collections.emptyList();
    }
}
