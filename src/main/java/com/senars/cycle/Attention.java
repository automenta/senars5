package com.senars.cycle;

import com.senars.core.Thought;

import java.util.Optional;

/**
 * Interface for the Attention Funnel, which is responsible for continuously
 * evaluating all candidate Thoughts and selecting the single one with the
 * highest Salience to become the focus of the current cognitive cycle.
 */
public interface Attention {

    /**
     * Adds a new Thought to the pool of candidates for attention.
     *
     * @param thought The thought to add.
     */
    void addCandidate(Thought thought);

    /**
     * Selects the single Thought with the highest Salience from the candidate pool.
     * The selected Thought is removed from the funnel.
     *
     * @return An Optional containing the highest-salience Thought, or empty if no
     *         candidates are available.
     */
    Optional<Thought> selectFocusThought();
}
