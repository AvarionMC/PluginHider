package org.avarion.plugin_hider.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    private Version versionUnderTest;

    @BeforeEach
    void setUp() {
        versionUnderTest = new Version("1.0.0-SNAPSHOT");
    }

    @Test
    void testCompareTo() {
        // Setup
        final Version other = new Version("1.0.0-TEST");

        // Run the test
        final int result = versionUnderTest.compareTo(other);

        // Verify the results
        assertEquals(0, result);
    }

    @Test
    void testCompareTo_ThrowsNullPointerException() {
        // Setup
        final Version other = new Version("version");

        // Run the test
        assertThrows(NullPointerException.class, () -> versionUnderTest.compareTo(other));
    }

    @Test
    void testCompareTo_ThrowsClassCastException() {
        // Setup
        final Version other = new Version("version");

        // Run the test
        assertThrows(ClassCastException.class, () -> versionUnderTest.compareTo(other));
    }

    @Test
    void testEquals() {
        assertFalse(versionUnderTest.equals("obj"));
    }

    @Test
    void testHashCode() {
        assertEquals(0, versionUnderTest.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("result", versionUnderTest.toString());
    }
}
