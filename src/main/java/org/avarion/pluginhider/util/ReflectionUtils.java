package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class for reflection operations
 */
public class ReflectionUtils {
    private static final Map<String, Class<?>> classCache = new HashMap<>();

    /**
     * Gets value of a private field via reflection
     */
    public static <T> T getStaticFieldValue(
            @NotNull Class<?> currentClass,
            String fieldName,
            @NotNull Class<T> fieldType
    ) {
        try {
            Field field = currentClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(null));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to access " + fieldName + " in " + currentClass.getName(), e);
        }
    }

    public static @NotNull @Unmodifiable List<Field> getFields(@NotNull Class<?> clazz) {
        try {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }
            return List.of(fields);
        }
        catch (SecurityException e) {
            throw new RuntimeException("Failed to access fields in " + clazz.getName(), e);
        }
    }

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

    /**
     * Gets a private method via reflection, checking all superclasses
     */
    public static @NotNull Method getMethod(@NotNull Object obj, String methodName, Class<?>... paramTypes) {
        Class<?> currentClass = obj.getClass();

        while (currentClass != null) {
            try {
                Method method = currentClass.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return method;
            }
            catch (NoSuchMethodException e) {
                // Method not found in current class, check superclass
                currentClass = currentClass.getSuperclass();
            }
            catch (SecurityException e) {
                throw new RuntimeException(
                        "Security exception accessing " + methodName + " in " + currentClass.getName(), e
                );
            }
        }

        throw new RuntimeException("Method "
                                   + methodName
                                   + " not found in "
                                   + obj.getClass().getName()
                                   + " or any superclass");
    }

    public static Class<?> getClass(String fullPath) {
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
     * Invokes a method found via reflection
     */
    public static Object invoke(@NotNull Object obj, String methodName, Object @NotNull ... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : null;
        }

        try {
            Method method = getMethod(obj, methodName, paramTypes);
            return method.invoke(obj, args);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to invoke method " + methodName + " on " + obj.getClass().getName(), e);
        }
    }

    /**
     * Invokes a method and casts the result to the specified type
     */
    public static <T> T invoke(@NotNull Object obj, String methodName, Class<T> returnType, Object... args) {
        Object result = invoke(obj, methodName, args);
        if (result == null && returnType.isPrimitive()) {
            throw new RuntimeException("Cannot cast null to primitive type " + returnType.getName());
        }
        return returnType.cast(result);
    }

    /**
     * Gets a static method from a class
     */
    public static @NotNull Method getStaticMethod(@NotNull Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Method " + methodName + " not found in " + clazz.getName(), e);
        }
    }

    /**
     * Invokes a static method via reflection
     */
    public static Object invokeStatic(@NotNull Class<?> clazz, String methodName, Object @NotNull ... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i] != null ? args[i].getClass() : null;
        }

        try {
            Method method = getStaticMethod(clazz, methodName, paramTypes);
            return method.invoke(null, args);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to invoke static method " + methodName + " on " + clazz.getName(), e);
        }
    }

    /**
     * Invokes a static method and casts the result to the specified type
     */
    public static <T> T invokeStatic(@NotNull Class<?> clazz, String methodName, Class<T> returnType, Object... args) {
        Object result = invokeStatic(clazz, methodName, args);
        if (result == null && returnType.isPrimitive()) {
            throw new RuntimeException("Cannot cast null to primitive type " + returnType.getName());
        }
        return returnType.cast(result);
    }
}
