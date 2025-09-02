package com.senars;

import com.senars.cycle.CognitiveCycle;
import com.senars.cycle.ShutdownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry point for the SeNARS Cognitive System application.
 * This class is responsible for initializing the system via the SystemFactory
 * and running the main cognitive cycle loop.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        LOGGER.info("Starting SeNARS Cognitive System...");

        SystemFactory factory = new SystemFactory();
        CognitiveCycle cognitiveCycle = factory.getCognitiveCycle();

        LOGGER.info("SeNARS Cognitive System Initialized. Starting cognitive cycle.");

        long stepCount = 0;
        try {
            while (true) {
                try {
                    LOGGER.info("--- Cycle Step {} ---", ++stepCount);
                    cognitiveCycle.step();
                    Thread.sleep(200); // Pause between cycles
                } catch (ShutdownException e) {
                    LOGGER.info("Shutdown command received. Terminating SeNARS.");
                    break;
                }
            }
        } finally {
            LOGGER.info("Persisting memory state...");
            factory.memory.persist();
            factory.eventBus.shutdown();
            LOGGER.info("SeNARS Cognitive System finished after {} steps.", stepCount);
        }
    }
}
