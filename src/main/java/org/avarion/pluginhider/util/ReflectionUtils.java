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
        Class<?> currentClass = obj.getClass();

        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return fieldType.cast(field.get(obj));
            }
            catch (NoSuchFieldException e) {
                // Field not found in current class, check superclass
                currentClass = currentClass.getSuperclass();
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access " + fieldName + " in " + obj.getClass().getName(), e);
            }
        }

        throw new RuntimeException("Field "
                                   + fieldName
                                   + " not found in "
                                   + obj.getClass().getName()
                                   + " or any superclass");
    }
}