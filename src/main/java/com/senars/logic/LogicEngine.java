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

    public LogicEngine(String theory) {
        this.prolog = new Prolog();
        try {
            prolog.setTheory(new Theory(theory));
        } catch (Exception e) { // Catching generic Exception to be refined later
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
