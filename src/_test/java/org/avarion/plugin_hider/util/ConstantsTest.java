package org.avarion.plugin_hider.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ConstantsTest {

    @Test
    void testIsPluginCmd() {
        assertFalse(Caches.isPluginCmd("txt"));
    }

    @Test
    void testIsVersionCmd() {
        assertFalse(Caches.isVersionCmd("txt"));
    }
}
