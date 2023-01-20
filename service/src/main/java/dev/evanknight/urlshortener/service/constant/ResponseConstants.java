package dev.evanknight.urlshortener.service.constant;

import lombok.AllArgsConstructor;

public class ResponseConstants {
    @AllArgsConstructor
    public enum HttpStatus {
        SUCCESS(200),
        MOVED(301),
        FOUND(302),
        BAD_REQUEST(400),
        NOT_FOUND(404);

        public final int code;
    }

    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String JSON_CONTENT = "application/json";
    public static final String HTML_CONTENT = "text/html";
    public static final String TEXT_CONTENT = "text/plain";

    public static final String LOCATION_HEADER = "Location";
}
