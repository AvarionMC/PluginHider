package org.avarion.pluginhider.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @Test
    void cleanupWordLowercasesFirstWord() {
        assertEquals("foo", Util.cleanupWord("Foo Bar"));
        assertEquals("plugin", Util.cleanupWord("PLUGIN"));
        assertEquals("tp", Util.cleanupWord("tp"));
    }

    @Test
    void cleanupWordHandlesNullAndEmpty() {
        assertEquals("", Util.cleanupWord(null));
        assertEquals("", Util.cleanupWord(""));
    }

    @Test
    void cleanupCommandStripsLeadingSlashAndArgs() {
        assertEquals("tp", Util.cleanupCommand("/tp here there"));
        assertEquals("tp", Util.cleanupCommand("tp"));
        assertEquals("give", Util.cleanupCommand("/Give player"));
    }

    @Test
    void cleanupCommandDoesNotThrowOnEmptyOrNull() {
        // Regression: charAt(0) on the cleaned-up empty string used to throw
        // StringIndexOutOfBoundsException.
        assertEquals("", Util.cleanupCommand(""));
        assertEquals("", Util.cleanupCommand(null));
        assertEquals("", Util.cleanupCommand("   /"));
    }
}
