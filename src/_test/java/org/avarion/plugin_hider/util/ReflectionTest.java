package org.avarion.plugin_hider.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReflectionTest {

    @Test
    void testGetClass() {
        assertEquals(String.class, Reflection.getClass("classPath"));
    }

    @Test
    void testCallStatic1() {
        // Setup
        final Method method = null;

        // Run the test
        final String result = Reflection.callStatic(method, "params");

        // Verify the results
        assertEquals("result", result);
    }

    @Test
    void testCallStatic2() {
        assertEquals("result", Reflection.callStatic(String.class, "methodName"));
    }

    @Test
    void testGetMethod() {
        // Setup
        final Method expectedResult = null;

        // Run the test
        final Method result = Reflection.getMethod(String.class, "methodName", String.class);

        // Verify the results
        assertEquals(expectedResult, result);
    }
}
