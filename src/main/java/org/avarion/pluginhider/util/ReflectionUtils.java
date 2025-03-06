package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;


/**
 * Utility class for reflection operations
 */
public class ReflectionUtils {
    /**
     * Gets value of a private field via reflection
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