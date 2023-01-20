package dev.evanknight.urlshortener.tests.util;

import com.google.gson.Gson;

public class SerializationUtils {
    private static final Gson GSON = new Gson();

    public static String toJson(final Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJson(final String json, final Class<T> target) {
        return GSON.fromJson(json, target);
    }

}
