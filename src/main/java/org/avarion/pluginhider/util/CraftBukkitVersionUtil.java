package org.avarion.pluginhider.util;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for handling CraftBukkit versioned classes
 */


/**
 * Utility for handling CraftBukkit versioned classes
 */
public class CraftBukkitVersionUtil {
    private static final String CRAFTBUKKIT_PATH;
    private static final Map<String, Class<?>> classCache = new HashMap<>();

    static {
        // org.bukkit.craftbukkit.v1_16_R1 on v1.16.1
        // org.bukkit.craftbukkit on v1.21.1
        CRAFTBUKKIT_PATH = Bukkit.getServer().getClass().getPackage().getName();
    }

    /**
     * Gets the properly versioned CraftBukkit class
     */
    public static Class<?> getCraftBukkitClass(String unversionedPath) {
        String fullPath = CRAFTBUKKIT_PATH + "." + unversionedPath;

        return classCache.computeIfAbsent(
                fullPath, path -> {
                    try {
                        return Class.forName(path);
                    }
                    catch (ClassNotFoundException e) {
                        return null;
                    }
                }
        );
    }

    /**
     * Checks if an object is an instance of a versioned CraftBukkit class
     *
     * @param obj Object to check
     * @param unversionedPath Unversioned class path (e.g., "help.CommandAliasHelpTopic")
     * @return true if obj is an instance of the specified class
     */
    public static boolean isInstance(Object obj, String unversionedPath) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = getCraftBukkitClass(unversionedPath);
        return clazz != null && clazz.isInstance(obj);
    }
}