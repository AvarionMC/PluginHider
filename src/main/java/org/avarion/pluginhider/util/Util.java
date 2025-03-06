package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


public class Util {
    private static final LRUCache<String, String> cacheCommand = new LRUCache<>(1_000);
    private static final LRUCache<String, String> cacheWord = new LRUCache<>(1_000);

    private Util() {
    }

    /**
     * Normalizes a command string by keeping the first word, converted to lowercase,
     * and removing leading slash if present.
     */
    public static @NotNull String cleanupCommand(@Nullable String command) {
        return cacheCommand.computeIfAbsent(
                command, cmd -> {
                    cmd = cleanupWord(cmd);
                    if (cmd.charAt(0) == '/') {
                        return cmd.substring(1);
                    }
                    return cmd;
                }
        );
    }

    public static @NotNull String cleanupWord(@Nullable final String text) {
        return cacheWord.computeIfAbsent(
                text, cmd -> {
                    if (cmd == null || cmd.isEmpty()) {
                        return "";
                    }

                    int index = cmd.indexOf(' ');
                    if (index != -1) {
                        cmd = cmd.substring(0, index); // First word only
                    }

                    return cmd.toLowerCase(Locale.ENGLISH);
                }
        );
    }
}
