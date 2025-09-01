package com.senars.motive;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the system's will, providing top-down purpose that shapes Salience
 * calculations and guides all cognitive activity.
 */
public class MotiveHierarchy {

    // Top-level, permanent, intrinsic needs.
    private final List<Thought> drives;

    // Mid-level, long-term goals. Using CopyOnWriteArrayList for thread-safety.
    private final List<Thought> ambitions = new CopyOnWriteArrayList<>();

    // Low-level, current goal. Can be null if the system is idle.
    private volatile Thought intention;

    /**
     * Default constructor, initializes with no drives.
     */
    public MotiveHierarchy() {
        this.drives = Collections.emptyList();
    }

    /**
     * Constructor that initializes the hierarchy with a set of drives.
     *
     * @param drives The list of drive thoughts.
     */
    public MotiveHierarchy(List<Thought> drives) {
        this.drives = drives != null ? List.copyOf(drives) : Collections.emptyList();
    }

    /**
     * Gets an unmodifiable list of the system's intrinsic Drives.
     *
     * @return The list of drive thoughts.
     */
    public List<Thought> getDrives() {
        return drives;
    }

    /**
     * Sets a new Ambition. Ambitions must be GOAL thoughts.
     *
     * @param ambition The GOAL thought to add as an ambition.
     * @throws IllegalArgumentException if the thought is not of type GOAL.
     */
    public void addAmbition(Thought ambition) {
        if (ambition.metadata().type() != ThoughtType.GOAL) {
            throw new IllegalArgumentException("Ambitions must be of type GOAL.");
        }
        this.ambitions.add(ambition);
    }

    /**
     * Removes an Ambition.
     *
     * @param ambitionId The ID of the ambition to remove.
     * @return true if the ambition was removed, false otherwise.
     */
    public boolean removeAmbition(String ambitionId) {
        return this.ambitions.removeIf(ambition -> ambition.id().equals(ambitionId));
    }

    /**
     * Gets an unmodifiable list of the current Ambitions.
     *
     * @return The list of ambition thoughts.
     */
    public List<Thought> getAmbitions() {
        return Collections.unmodifiableList(ambitions);
    }

    /**
     * Sets the current Intention. The intention must be a GOAL thought.
     *
     * @param intention The GOAL thought to set as the current intention.
     * @throws IllegalArgumentException if the thought is not of type GOAL.
     */
    public void setIntention(Thought intention) {
        if (intention != null && intention.metadata().type() != ThoughtType.GOAL) {
            throw new IllegalArgumentException("Intention must be of type GOAL.");
        }
        this.intention = intention;
    }

    /**
     * Clears the current intention.
     */
    public void clearIntention() {
        this.intention = null;
    }

    /**
     * Gets the current Intention.
     *
     * @return An Optional containing the current intention thought, or empty if none is set.
     */
    public Optional<Thought> getIntention() {
        return Optional.ofNullable(intention);
    }
}
