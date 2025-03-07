package org.avarion.pluginhider.custom_commands;

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
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.defaults.HelpCommand;
import org.bukkit.help.*;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.avarion.pluginhider.util.CraftBukkitVersionUtil.isInstance;


public class CustomHelpCommand extends HelpCommand implements MyCustomCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String currentAlias, @NotNull String[] args) {
        Caches.load();

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
                topic = this.findPossibleMatches(sender, command);
            }

            topic = filterTopic(sender, topic);

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

    @Contract("_, null -> false")
    protected boolean isAllowed(@NotNull CommandSender sender, @Nullable HelpTopic topic) {
        if (topic == null || !topic.canSee(sender)) {
            return false;
        }

        if (topic instanceof IndexHelpTopic) {
            return topic.getName().equalsIgnoreCase("Aliases") || isAllowedPlugin(sender, topic.getName());
        }
        else if (topic instanceof GenericCommandHelpTopic || isInstance(topic, "help.CommandAliasHelpTopic")) {
            return isAllowedCommand(sender, topic.getName());
        }
        else {
            PluginHider.logger.error("Unknown topic type: " + topic.getClass().getName());
            return false;
        }
    }

    @Contract("_, null -> null")
    private @Nullable HelpTopic filterTopic(@NotNull CommandSender sender, @Nullable HelpTopic topic) {
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
                if (isAllowed(sender, oldTopic)) {
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
        else if (!isAllowed(sender, topic)) {
            return null;
        }
        else {
            return topic;
        }
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Caches.load();

        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");
        if (args.length == 1) {
            List<String> matchedTopics = new ArrayList<>();
            String searchString = args[0];

            for (HelpTopic topic : Bukkit.getServer().getHelpMap().getHelpTopics()) {
                if (!isAllowed(sender, topic)) {
                    continue;
                }

                String trimmedTopic = topic.getName().startsWith("/") ? topic.getName().substring(1) : topic.getName();
                if (trimmedTopic.startsWith(searchString)) {
                    matchedTopics.add(trimmedTopic);
                }
            }

            return matchedTopics;
        }

        return ImmutableList.of();
    }

    @Nullable
    protected HelpTopic findPossibleMatches(@NotNull CommandSender sender, @NotNull String searchString) {
        int maxDistance = (searchString.length() / 5) + 3;
        Set<HelpTopic> possibleMatches = new TreeSet<HelpTopic>(HelpTopicComparator.helpTopicComparatorInstance());

        if (searchString.startsWith("/")) {
            searchString = searchString.substring(1);
        }

        if (searchString.isEmpty()) {
            return null; // Paper - prevent index out of bounds - nothing matches an empty search string, should have been special cased to defaultTopic earlier, just return null.
        }
        for (HelpTopic topic : Bukkit.getServer().getHelpMap().getHelpTopics()) {
            if (!isAllowed(sender, topic)) {
                continue;
            }

            String trimmedTopic = topic.getName().startsWith("/") ? topic.getName().substring(1) : topic.getName();

            if (trimmedTopic.length() < searchString.length()) {
                continue;
            }

            if (Character.toLowerCase(trimmedTopic.charAt(0)) != Character.toLowerCase(searchString.charAt(0))) {
                continue;
            }

            if (damerauLevenshteinDistance(searchString, trimmedTopic.substring(0, searchString.length()))
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
