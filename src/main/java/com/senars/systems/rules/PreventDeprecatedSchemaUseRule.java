package com.senars.systems.rules;

import com.senars.core.Thought;
import com.senars.core.ThoughtType;
import com.senars.logic.LogicEngine;
import com.senars.systems.Memory;
import com.senars.systems.Rule;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A rule that vetoes an action plan if its provenance trace contains a deprecated schema.
 */
public class PreventDeprecatedSchemaUseRule implements Rule {

    private final Memory memory;
    private final LogicEngine logicEngine;

    public PreventDeprecatedSchemaUseRule(Memory memory, LogicEngine logicEngine) {
        this.memory = Objects.requireNonNull(memory);
        this.logicEngine = Objects.requireNonNull(logicEngine);
    }

    @Override
    public Optional<String> check(Thought actionPlan) {
        if (actionPlan == null || actionPlan.metadata() == null || actionPlan.metadata().trace() == null) {
            return Optional.empty();
        }

        List<String> traceIds = actionPlan.metadata().trace();
        for (String thoughtId : traceIds) {
            Optional<Thought> thoughtOpt = memory.getThoughtById(thoughtId);
            if (thoughtOpt.isPresent()) {
                Thought thought = thoughtOpt.get();
                if (thought.metadata().type() == ThoughtType.SCHEMA) {
                    // Check the logic engine if this schema is deprecated
                    String query = String.format("is_deprecated('%s')", thought.id());
                    if (!logicEngine.solve(query).isEmpty()) {
                        return Optional.of("Action plan vetoed because it was derived from a deprecated schema: " + thought.id());
                    }
                }
            }
        }

        return Optional.empty();
    }
}
