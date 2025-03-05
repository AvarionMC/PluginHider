package org.avarion.plugin_hider.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ConstantsTest {
    @Test
    void testShouldNotHideThisCommand() {
        assertFalse(Constants.shouldHideThisCommand("txt"));
    }

    @Test
    void testShouldHideThisCommand() {
        assertFalse(Constants.shouldHideThisCommand("pl"));
    }
}
