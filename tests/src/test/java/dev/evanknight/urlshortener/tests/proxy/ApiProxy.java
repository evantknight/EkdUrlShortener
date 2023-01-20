package dev.evanknight.urlshortener.tests.proxy;

import dev.evanknight.urlshortener.service.model.WriteLambdaRequest;
import dev.evanknight.urlshortener.tests.util.SerializationUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import static dev.evanknight.urlshortener.constants.SysEnv.API_ENDPOINT;

public class ApiProxy {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final String DEFAULT_ENDPOINT = "https://short.evanknight.dev";
    private static final String ENDPOINT_FROM_ENV = System.getenv(API_ENDPOINT);
    private static final String ENDPOINT = ENDPOINT_FROM_ENV == null ? DEFAULT_ENDPOINT : ENDPOINT_FROM_ENV;
    private static final URI API_URI = URI.create(ENDPOINT);

    public HttpResponse<String> get(final String shortUrl) throws Exception {
        final URI uri = API_URI.resolve("/" + shortUrl);
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
        return HTTP_CLIENT.send(request, BodyHandlers.ofString());
    }

    public HttpResponse<String> post(final String longUrl) throws Exception {
        final String body = SerializationUtils.toJson(new WriteLambdaRequest(longUrl));
        return postWithBody(body);
    }

    public HttpResponse<String> postWithBody(final String body) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(API_URI)
                .POST(BodyPublishers.ofString(body))
                .build();
        return HTTP_CLIENT.send(request, BodyHandlers.ofString());
    }

}
