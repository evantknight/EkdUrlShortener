package dev.evanknight.urlshortener.service.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class SerializationUtils {

    private static final Gson GSON = new Gson();

    private static final TypeToken<Map<String, String>> MAP_TYPE = new TypeToken<>(){};


    public static Map<String, String> jsonToMap(final String json) {
        return GSON.fromJson(json, MAP_TYPE);
    }

    public static String toJson(final Object source) {
        return GSON.toJson(source);
    }

    public static String toJson(final String key, final String value) {
        return toJson(Map.of(key, value));
    }

}
