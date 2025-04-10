package com.joevtap.jmsapp;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Utility class for handling math calculations
 */
public class MathProcessor {

    /**
     * Evaluates a mathematical expression using the GraalVM JavaScript engine
     */
    public static String calculate(String expression) {
        // Suppress GraalVM warnings
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        System.setProperty("polyglot.log.file", "jms_graal.log");

        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("graal.js");

        try {
            Object result = engine.eval(expression);
            return result.toString();
        } catch (ScriptException e) {
            return "Calculation error: " + e.getMessage();
        }
    }
}