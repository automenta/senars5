package com.senars.lm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DefaultToolExecutor implements ToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultToolExecutor.class);
    private final List<Object> tools;
    private final Gson gson = new Gson();

    public DefaultToolExecutor(List<Object> tools) {
        this.tools = tools;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object context) {
        LOGGER.debug("Attempting to execute tool request: {}", toolExecutionRequest.name());
        for (Object tool : tools) {
            for (Method method : tool.getClass().getMethods()) {
                if (method.getName().equals(toolExecutionRequest.name())) {
                    try {
                        LOGGER.debug("Found matching method: {}.{}", tool.getClass().getSimpleName(), method.getName());
                        Type type = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> arguments = gson.fromJson(toolExecutionRequest.arguments(), type);
                        LOGGER.debug("Parsed arguments: {}", arguments);

                        Object[] args = new Object[method.getParameterCount()];
                        if (method.getParameterCount() == 1) {
                            // Workaround for -parameters flag not working
                            args[0] = arguments.values().iterator().next();
                        } else {
                            Parameter[] parameters = method.getParameters();
                            for (int i = 0; i < parameters.length; i++) {
                                String paramName = parameters[i].getName();
                                Object argValue = arguments.get(paramName);
                                if (argValue == null) {
                                    args[i] = null;
                                } else if (parameters[i].getType().equals(String.class)) {
                                    args[i] = String.valueOf(argValue);
                                } else {
                                    args[i] = argValue;
                                }
                            }
                        }

                        LOGGER.debug("Invoking method with args: {}", Arrays.toString(args));
                        Object result = method.invoke(tool, args);
                        return result.toString();
                    } catch (Exception e) {
                        LOGGER.error("Error executing tool {}", toolExecutionRequest.name(), e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        throw new RuntimeException("Tool method not found: " + toolExecutionRequest.name());
    }
}
