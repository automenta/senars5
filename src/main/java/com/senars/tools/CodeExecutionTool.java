package com.senars.tools;

import dev.langchain4j.agent.tool.Tool;

/**
 * A stub tool for executing code.
 * In a full implementation, this would interface with a sandboxed execution environment.
 * For now, it informs the user that the feature is not implemented.
 */
public class CodeExecutionTool {

    @Tool("Executes a block of code. This tool is currently a stub and not functional.")
    public String execute(String code) {
        return "Error: Code execution is not implemented in this version.";
    }
}
