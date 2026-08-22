package org.avarion.pluginhider.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {
    @Test
    void parsesFullVersion() {
        Version v = new Version("v1.2.3");
        assertEquals(1, v.major);
        assertEquals(2, v.minor);
        assertEquals(3, v.patch);
        assertEquals("1.2.3", v.toString());
    }

    @Test
    void parsesPartialAndMissingComponents() {
        assertEquals("1.2.0", new Version("1.2").toString());
        assertEquals("1.0.0", new Version("1").toString());
        assertEquals("1.2.3", new Version("1.2.3-SNAPSHOT").toString());
    }

    @Test
    void nullAndGarbageBecomeZero() {
        assertEquals("0.0.0", new Version(null).toString());
        assertEquals("0.0.0", new Version("garbage").toString());
        assertEquals("0.0.0", new Version("").toString());
    }

    @Test
    void comparesByComponent() {
        assertTrue(new Version("1.2.3").compareTo(new Version("1.2.0")) > 0);
        assertTrue(new Version("2.0.0").compareTo(new Version("1.9.9")) > 0);
        assertEquals(0, new Version("1.2.3").compareTo(new Version("1.2.3")));
    }

    @Test
    void equalsAndHashCode() {
        assertEquals(new Version("1.2.3"), new Version("v1.2.3"));
        assertEquals(new Version("1.2.3").hashCode(), new Version("1.2.3").hashCode());
        assertNotEquals(new Version("1.2.3"), new Version("1.2.4"));
    }
}
