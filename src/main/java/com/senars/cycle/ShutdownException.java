package com.senars.cycle;

/**
 * An exception used to signal a graceful shutdown of the cognitive cycle.
 */
public class ShutdownException extends Exception {
    public ShutdownException(String message) {
        super(message);
    }
}
