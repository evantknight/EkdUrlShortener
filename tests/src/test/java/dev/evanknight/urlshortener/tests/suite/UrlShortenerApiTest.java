package dev.evanknight.urlshortener.tests.suite;

import dev.evanknight.urlshortener.service.model.WriteLambdaResponse;
import dev.evanknight.urlshortener.tests.proxy.ApiProxy;
import dev.evanknight.urlshortener.tests.util.SerializationUtils;
import org.testng.annotations.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UrlShortenerApiTest {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String HTML_CONTENT = "text/html";
    private static final String TEXT_CONTENT = "text/plain";
    private static final String LOCATION = "Location";

    private final ApiProxy apiProxy = new ApiProxy();

    @Test
    public void post_ValidUrl_Success() throws Exception {
        // Arrange.
        final String longUrl = "https://evanknight.dev/";

        // Act.
        final HttpResponse<String> response = apiProxy.post(longUrl);

        // Assert.
        final WriteLambdaResponse pojo = SerializationUtils.fromJson(response.body(), WriteLambdaResponse.class);
        assertEquals(response.statusCode(), 200);
        assertFalse(pojo.getShortUrl().isBlank());
    }

    @Test
    public void post_InvalidUrl_Success() throws Exception {
        // Arrange.
        // URL is missing protocol, therefore invalid
        final String longUrl = "example.com";

        // Act.
        final HttpResponse<String> response = apiProxy.post(longUrl);

        // Assert.
        final WriteLambdaResponse pojo = SerializationUtils.fromJson(response.body(), WriteLambdaResponse.class);
        assertEquals(response.statusCode(), 200);
        assertFalse(pojo.getShortUrl().isBlank());
    }

    @Test
    public void post_BadRequest_InvalidRequest() throws Exception {
        // Arrange.
        final String body = "test bad request";

        // Act.
        final HttpResponse<String> response = apiProxy.postWithBody(body);

        // Assert.
        assertEquals(response.statusCode(), 400);
        final Map<String, List<String>> headers = response.headers().map();
        assertTrue(headers.containsKey(CONTENT_TYPE));
        final List<String> contentTypeList = headers.get(CONTENT_TYPE);
        assertEquals(contentTypeList.size(), 1);
        assertEquals(contentTypeList.get(0), TEXT_CONTENT);
        assertFalse(response.body().isBlank());
    }

    @Test
    public void get_NoPath_Index() throws Exception {
        // Arrange.
        final String shortUrl = "";

        // Act.
        final HttpResponse<String> response = apiProxy.get(shortUrl);

        // Assert.
        assertEquals(response.statusCode(), 200);
        final Map<String, List<String>> headers = response.headers().map();
        assertTrue(headers.containsKey(CONTENT_TYPE));
        final List<String> contentTypeList = headers.get(CONTENT_TYPE);
        assertEquals(contentTypeList.size(), 1);
        assertEquals(contentTypeList.get(0), HTML_CONTENT);
        assertFalse(response.body().isBlank());
    }

    @Test
    public void get_UrlMissing_404() throws Exception {
        // Arrange.
        final String shortUrl = "test-url-missing";

        // Act.
        final HttpResponse<String> response = apiProxy.get(shortUrl);

        // Assert.
        assertEquals(response.statusCode(), 404);
        final Map<String, List<String>> headers = response.headers().map();
        assertTrue(headers.containsKey(CONTENT_TYPE));
        final List<String> contentTypeList = headers.get(CONTENT_TYPE);
        assertEquals(contentTypeList.size(), 1);
        assertEquals(contentTypeList.get(0), HTML_CONTENT);
        assertFalse(response.body().isBlank());
    }

    @Test
    public void get_UrlPresent_Redirect() throws Exception {
        // Arrange.
        final String longUrl = "https://example.com/";
        final HttpResponse<String> postResponse = apiProxy.post(longUrl);
        final WriteLambdaResponse writeLambdaResponse = SerializationUtils.fromJson(postResponse.body(), WriteLambdaResponse.class);
        final String shortUrl = writeLambdaResponse.getShortUrl();

        // Act.
        final HttpResponse<String> response = apiProxy.get(shortUrl);

        // Assert.
        assertEquals(response.statusCode(), 302);
        final Map<String, List<String>> headers = response.headers().map();
        assertTrue(headers.containsKey(LOCATION));
        final List<String> locationList = headers.get(LOCATION);
        assertEquals(locationList.size(), 1);
        assertEquals(locationList.get(0), longUrl);
    }

}
