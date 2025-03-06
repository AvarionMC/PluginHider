package org.avarion.pluginhider.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility for handling CraftBukkit versioned classes
 */
public class CraftBukkitVersionUtil {
    private static final String SERVER_VERSION;
    private static final Map<String, Class<?>> classCache = new HashMap<>();

    static {
        // Extract server version from package name
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        SERVER_VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    /**
     * Gets the server version string (e.g., "v1_21_R1")
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    /**
     * Gets the properly versioned CraftBukkit class
     */
    public static Class<?> getCraftBukkitClass(String unversionedPath) {
        String fullPath = "org.bukkit.craftbukkit." + SERVER_VERSION + "." + unversionedPath;

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
     * Checks if a versioned CraftBukkit class exists
     */
    public static boolean craftBukkitClassExists(String unversionedPath) {
        return getCraftBukkitClass(unversionedPath) != null;
    }

    /**
     * Checks if an object is an instance of a versioned CraftBukkit class
     *
     * @param obj             Object to check
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

    /**
     * Gets an instance of a field from an object, handling version-specific classes
     */
    public static <T> T getFieldValue(@NotNull Object obj, String fieldName, @NotNull Class<T> fieldType) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(obj));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access " + fieldName + " in " + obj.getClass().getName(), e);
        }
    }
}