package org.avarion.pluginhider.util;

import org.bukkit.command.CommandSender;

public class SendCmd {
    public static void sendMessage(CommandSender cmdSender, String msg) {
        cmdSender.sendMessage(Constants.PREFIX + msg);
    }
}
