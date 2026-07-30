package com.opsfactor.community.platform.utility.Logger;

import java.util.logging.Logger;

public class LoggerImplementation {
    public final static boolean DEBUG = java.lang.management.ManagementFactory.getRuntimeMXBean().
            getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
    private static Logger LOGGER;

    static {
        String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

        LOGGER = Logger.getLogger(fullClassName + ":" + className + methodName + "():" + lineNumber);
    }

    /**
     * @param message breve informação da classe
     */
    public static void output(String message) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            LOGGER = Logger.getLogger(className + methodName + "():" + lineNumber);
            LOGGER.warning(message);
        }
    }

    /**
     * @param message mostra informações mais detalhadas do print
     */
    public static void info(String message) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            LOGGER = Logger.getLogger("Classname:" + fullClassName + "\n" + methodName + "():" + lineNumber);
            LOGGER.info(message);
        }
    }

    public static void error(String message) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            LOGGER = Logger.getLogger("Classname:" + fullClassName + "\n" + methodName + "():" + lineNumber);
            LOGGER.severe(message);
        }
    }

    public static void errorTest(String message, Exception e) {
        if (DEBUG) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            LOGGER = Logger.getLogger("Classname:" + fullClassName + "\n" + methodName + "():" + lineNumber);
            LOGGER.severe(message);
            LOGGER.severe(e.getStackTrace().toString());
        }
    }

    public static void print(Object message) {
        String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf("."));
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

        LOGGER = Logger.getLogger("Classname:" + className + "." + methodName + "():" + lineNumber);
        LOGGER.info(message.toString());
    }
}
