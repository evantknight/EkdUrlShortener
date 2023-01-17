package dev.evanknight.urlshortener.infrastructure.util;

public class NameUtils {
    private static final String PROJECT_NAME = "UrlShortener";

    public static String getStackName(final Class clazz) {
        return PROJECT_NAME + "-" + clazz.getSimpleName();
    }

}
