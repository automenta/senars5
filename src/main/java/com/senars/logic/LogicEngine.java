package com.senars.logic;

import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for the tuProlog engine, providing a simple interface for logical inference.
 * This class handles loading Prolog theories (facts and rules) and executing queries.
 */
public class LogicEngine {

    private final Prolog prolog;

    /**
     * Default constructor for creating an empty LogicEngine.
     */
    public LogicEngine() {
        this.prolog = new Prolog();
    }

    public LogicEngine(String theory) {
        this();
        try {
            prolog.setTheory(new Theory(theory));
        } catch (Exception e) { // Catching generic Exception to be refined later
            throw new RuntimeException("Invalid Prolog theory", e);
        }
    }

    /**
     * Adds a new fact or rule to the existing theory.
     * @param fact The fact or rule to add, e.g., "is_deprecated('schema-123')."
     */
    public void assertFact(String fact) {
        try {
            prolog.addTheory(new Theory(fact));
        } catch (Exception e) { // TODO: Use a more specific exception
            throw new RuntimeException("Invalid fact or rule: " + fact, e);
        }
    }

    /**
     * Replaces the entire current theory with a new one.
     * @param theory The new theory to load.
     */
    public void setTheory(String theory) {
        try {
            prolog.setTheory(new Theory(theory));
        } catch (Exception e) { // TODO: Use a more specific exception
            throw new RuntimeException("Invalid Prolog theory", e);
        }
    }

    public List<Map<String, String>> solve(String query) {
        List<Map<String, String>> solutions = new ArrayList<>();
        try {
            SolveInfo info = prolog.solve(query);
            while (info.isSuccess()) {
                Map<String, String> solution = new HashMap<>();
                for (Var var : info.getBindingVars()) {
                    solution.put(var.getName(), var.getTerm().toString());
                }
                solutions.add(solution);
                if (prolog.hasOpenAlternatives()) {
                    info = prolog.solveNext();
                } else {
                    break;
                }
            }
        } catch (Exception e) { // Catching generic Exception to be refined later
            throw new RuntimeException("Malformed Prolog goal", e);
        }
        return solutions;
    }
}
