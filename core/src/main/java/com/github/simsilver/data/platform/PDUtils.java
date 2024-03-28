package com.github.simsilver.data.platform;

import java.lang.reflect.InvocationTargetException;

public class PDUtils {

    public static final String PkgName = "com.github.simsilver.data";
    private static PDInterface instance;

    static {
        try {
            Class<?> clz = Class.forName(PkgName + ".platform.PSUtils");
            Object t = clz.getDeclaredConstructor().newInstance();
            if (t instanceof PDInterface) {
                instance = (PDInterface) t;
            }
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logD(String tag, String format, Object... args) {
        if (instance != null)
            instance.logD(tag, format, args);
    }
}
