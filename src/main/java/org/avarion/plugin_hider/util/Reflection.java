package org.avarion.plugin_hider.util;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Reflection {
    private static final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(String classPath) {
        if (!classes.containsKey(classPath)) {
            try {
                final Class<?> clazz = Class.forName(classPath);
                classes.put(classPath, clazz);
            }
            catch (ClassNotFoundException ignored) {
                throw new RuntimeException("Unknown class: " + classPath);
            }
        }

        return (Class<T>) classes.get(classPath);
    }

    @SuppressWarnings("unchecked")
    public static <T> T callStatic(Method method, Object... params) {
        try {
            return (T) method.invoke(null, params);
        }
        catch (Throwable ignored) {
            throw new RuntimeException("Can't call static method: " + method);
        }
    }

    public static Object callStatic(Class<?> clazz, String methodName) {
        return callStatic(getMethod(clazz, methodName));
    }

    public static @Nullable Method getMethod(final Class<?> clazz, String methodName, final Class<?>... args) {
        try {
            final Method method = clazz.getMethod(methodName, args);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException ignored) {
        }

        return null;
    }
}
