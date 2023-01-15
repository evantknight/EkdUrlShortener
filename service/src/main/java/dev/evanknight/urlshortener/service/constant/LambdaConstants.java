package dev.evanknight.urlshortener.service.constant;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class LambdaConstants {
    public static final String BODY_KEY = "body";
    public static final String LONG_URL_KEY = "longUrl";
    public static final String SHORT_URL_KEY = "shortUrl";
    public static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
}
