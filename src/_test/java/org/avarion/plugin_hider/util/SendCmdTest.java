package org.avarion.plugin_hider.util;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class SendCmdTest {

    @Test
    void testSendMessage() {
        // Setup
        final CommandSender cmdSender = null;

        // Run the test
        SendCmd.sendMessage(cmdSender, "msg");

        // Verify the results
    }
}
