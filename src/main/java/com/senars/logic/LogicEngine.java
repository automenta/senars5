package com.senars.logic;

import alice.tuprolog.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper for the tuProlog engine, providing a simple interface for logical inference.
 * This class handles loading Prolog theories (facts and rules) and executing queries.
 */
public class LogicEngine {

    private final Prolog engine;

    public LogicEngine(String theory) {
        this.engine = new Prolog();
        try {
            Theory t = new Theory(theory);
            engine.setTheory(t);
        } catch (Exception e) { // Catching generic Exception to be refined later
            throw new RuntimeException("Invalid Prolog theory", e);
        }
    }

    public List<Map<String, String>> solve(String query) {
        List<Map<String, String>> solutions = new ArrayList<>();
        try {
            SolveInfo info = engine.solve(query);
            while (info.isSuccess()) {
                Map<String, String> solution = new HashMap<>();
                for (Var var : info.getBindingVars()) {
                    solution.put(var.getName(), var.getTerm().toString());
                }
                solutions.add(solution);
                if (engine.hasOpenAlternatives()) {
                    info = engine.solveNext();
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
