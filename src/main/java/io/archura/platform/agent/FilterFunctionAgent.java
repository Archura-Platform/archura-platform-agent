package io.archura.platform.agent;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

import static java.util.Objects.nonNull;

public class FilterFunctionAgent {

    private static final Logger log = Logger.getLogger(FilterFunctionAgent.class.getSimpleName());
    private static String executorClassName = "io.archura.platform.internal.FilterFunctionExecutor";

    public static void premain(
            final String argument,
            final Instrumentation instrumentation
    ) {
        if (nonNull(argument) && !argument.trim().isEmpty()) {
            executorClassName = argument;
        }
        log.info("Filter and Function agent started with argument: " + executorClassName);
        instrumentation.addTransformer(new FilterFunctionTransformer(executorClassName));
    }
}
