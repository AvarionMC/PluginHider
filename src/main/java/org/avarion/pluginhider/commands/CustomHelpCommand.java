package org.avarion.pluginhider.commands;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.avarion.pluginhider.PluginHider;
import org.avarion.pluginhider.util.Caches;
import org.avarion.pluginhider.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.HelpCommand;
import org.bukkit.help.*;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

import static org.avarion.pluginhider.util.CraftBukkitVersionUtil.isInstance;


public class CustomHelpCommand extends HelpCommand {

    private @Nullable CommandMap getCommandMap() {
        try {
            final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void unregisterCommand(CommandMap commandMap, @NotNull Command cmd) {
        @SuppressWarnings("unchecked") Map<String, Command>
                knownCommands
                = (Map<String, Command>) ReflectionUtils.getFieldValue(commandMap, "knownCommands", Map.class);

        // Remove command and its aliases
        knownCommands.remove("help");
        knownCommands.remove("bukkit:help");
        knownCommands.remove("minecraft:help");

        // Also try to remove by aliases if any
        for (String alias : cmd.getAliases()) {
            knownCommands.remove(alias);
            knownCommands.remove("bukkit:" + alias);
            knownCommands.remove("minecraft:" + alias);
        }
    }

    public void replaceHelpCommand() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            PluginHider.logger.error("Could not access CommandMap - cannot register custom help command");
            return;
        }

        // Remove the existing "help" command
        Command existingCommand = commandMap.getCommand("help");
        if (existingCommand != null) {
            unregisterCommand(commandMap, existingCommand);
        }

        // Register your custom help command
        commandMap.register("", this);
        commandMap.register("bukkit", this);
        commandMap.register("minecraft", this);

        PluginHider.logger.info("Custom help command has been registered!");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        if (this.testPermission(sender)) {
            String command;
            int pageNumber;
            if (args.length == 0) {
                command = "";
                pageNumber = 1;
            }
            else if (NumberUtils.isDigits(args[args.length - 1])) {
                command = StringUtils.join(ArrayUtils.subarray(args, 0, args.length - 1), " ");

                try {
                    pageNumber = NumberUtils.createInteger(args[args.length - 1]);
                }
                catch (NumberFormatException var13) {
                    pageNumber = 1;
                }

                if (pageNumber <= 0) {
                    pageNumber = 1;
                }
            }
            else {
                command = StringUtils.join(args, " ");
                pageNumber = 1;
            }

            int pageHeight;
            int pageWidth;
            if (sender instanceof ConsoleCommandSender) {
                pageHeight = Integer.MAX_VALUE;
                pageWidth = Integer.MAX_VALUE;
            }
            else {
                pageHeight = 9;
                pageWidth = 55;
            }

            HelpMap helpMap = Bukkit.getServer().getHelpMap();
            HelpTopic topic = helpMap.getHelpTopic(command);
            if (topic == null) {
                topic = helpMap.getHelpTopic("/" + command);
            }

            if (topic == null) {
                topic = this.findPossibleMatches(command);
            }

            topic = filterTopic(topic);

            if (topic != null && topic.canSee(sender)) {
                ChatPaginator.ChatPage page = ChatPaginator.paginate(
                        topic.getFullText(sender),
                        pageNumber,
                        pageWidth,
                        pageHeight
                );
                StringBuilder header = new StringBuilder();
                header.append(ChatColor.YELLOW);
                header.append("--------- ");
                header.append(ChatColor.WHITE);
                header.append("Help: ");
                header.append(topic.getName());
                header.append(" ");
                if (page.getTotalPages() > 1) {
                    header.append("(");
                    header.append(page.getPageNumber());
                    header.append("/");
                    header.append(page.getTotalPages());
                    header.append(") ");
                }

                header.append(ChatColor.YELLOW);

                header.append("-".repeat(Math.max(0, 55 - header.length())));

                sender.sendMessage(header.toString());
                sender.sendMessage(page.getLines());
            }
            else {
                sender.sendMessage(ChatColor.RED + "No help for " + command);
            }
        }
        return true;
    }

    @Contract("null -> false")
    protected boolean isAllowed(@Nullable HelpTopic topic) {
        if (topic == null) {
            return false;
        }

        if (topic instanceof IndexHelpTopic) {
            return topic.getName().equalsIgnoreCase("Aliases") || Caches.shouldShowPlugin(topic.getName());
        }
        else if (topic instanceof GenericCommandHelpTopic || isInstance(topic, "help.CommandAliasHelpTopic")) {
            return Caches.shouldShowCommand(topic.getName());
        }
        else {
            PluginHider.logger.error("Unknown topic type: " + topic.getClass().getName());
            return false;
        }
    }

    @Contract("null -> null")
    private @Nullable HelpTopic filterTopic(@Nullable HelpTopic topic) {
        if (topic == null) {
            return null;
        }

        if (topic instanceof IndexHelpTopic) {
            List<HelpTopic> newAllTopics = new ArrayList<>();
            @SuppressWarnings("unchecked") Collection<HelpTopic> oldAllTopics = ReflectionUtils.getFieldValue(
                    topic,
                    "allTopics",
                    Collection.class
            );

            for (HelpTopic oldTopic : oldAllTopics) {
                if (isAllowed(oldTopic)) {
                    newAllTopics.add(oldTopic);
                }
            }

            if (newAllTopics.isEmpty()) {
                return null;
            }

            return new IndexHelpTopic(
                    topic.getName(),
                    topic.getShortText(),
                    ReflectionUtils.getFieldValue(topic, "permission", String.class),
                    newAllTopics,
                    ReflectionUtils.getFieldValue(topic, "preamble", String.class)
            );
        }
        else if (!isAllowed(topic)) {
            return null;
        }
        else {
            return topic;
        }
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");
        if (args.length == 1) {
            List<String> matchedTopics = new ArrayList<>();
            String searchString = args[0];

            for (HelpTopic topic : Bukkit.getServer().getHelpMap().getHelpTopics()) {
                if (!isAllowed(topic)) {
                    continue;
                }

                String trimmedTopic = topic.getName().startsWith("/") ? topic.getName().substring(1) : topic.getName();
                if (trimmedTopic.startsWith(searchString)) {
                    matchedTopics.add(trimmedTopic);
                }
            }

            return matchedTopics;
        }
        else {
            return ImmutableList.of();
        }
    }

    @Nullable
    protected HelpTopic findPossibleMatches(@NotNull String searchString) {
        int maxDistance = searchString.length() / 5 + 3;
        Set<HelpTopic> possibleMatches = new TreeSet<>(HelpTopicComparator.helpTopicComparatorInstance());
        if (searchString.startsWith("/")) {
            searchString = searchString.substring(1);
        }

        for (HelpTopic topic : Bukkit.getServer().getHelpMap().getHelpTopics()) {
            if (!isAllowed(topic)) {
                continue;
            }

            String trimmedTopic = topic.getName().startsWith("/") ? topic.getName().substring(1) : topic.getName();
            if (trimmedTopic.length() >= searchString.length()
                && Character.toLowerCase(trimmedTopic.charAt(0)) == Character.toLowerCase(searchString.charAt(0))
                && damerauLevenshteinDistance(searchString, trimmedTopic.substring(0, searchString.length()))
                   < maxDistance) {
                possibleMatches.add(topic);
            }
        }

        if (!possibleMatches.isEmpty()) {
            return new IndexHelpTopic("Search", null, null, possibleMatches, "Search for: " + searchString);
        }
        else {
            return null;
        }
    }
}
