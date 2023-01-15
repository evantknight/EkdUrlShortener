package dev.evanknight.urlshortener.service.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import dev.evanknight.urlshortener.service.proxy.DynamoDb;
import dev.evanknight.urlshortener.service.util.SerializationUtils;

import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

import static dev.evanknight.urlshortener.service.constant.LambdaConstants.LONG_URL_KEY;
import static dev.evanknight.urlshortener.service.constant.LambdaConstants.SHORT_URL_KEY;
import static dev.evanknight.urlshortener.service.constant.ResponseConstants.CONTENT_TYPE_HEADER;
import static dev.evanknight.urlshortener.service.constant.ResponseConstants.HttpStatus.BAD_REQUEST;
import static dev.evanknight.urlshortener.service.constant.ResponseConstants.HttpStatus.SUCCESS;
import static dev.evanknight.urlshortener.service.constant.ResponseConstants.JSON_CONTENT;

// TODO: Consider using unique id generator for shortUrl.
public class WriteLambda implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final String HTTPS = "https://";
    private static final Map<String, String> JSON_HEADER = Map.of(CONTENT_TYPE_HEADER, JSON_CONTENT);

    private static final String SHORT_URL_CHAR_SET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final BigInteger TARGET_BASE = new BigInteger(String.valueOf(SHORT_URL_CHAR_SET.length()));
    private static final int TARGET_LENGTH = 5;
    private static final String HASH_ALGO = "MD5";
    private static final byte[] APPEND_STRING = "bleh".getBytes(StandardCharsets.UTF_8);

    private static final DynamoDb DDB = new DynamoDb();

    // SnapStart Warmup
    static {
        DDB.getUrl("warmup");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(final APIGatewayV2HTTPEvent request, final Context context) {
        final Map<String, String> bodyJson = SerializationUtils.jsonToMap(request.getBody());
        final String longUrlRequest = bodyJson.get(LONG_URL_KEY);

        final String longUrl;
        if (isValidUrl(longUrlRequest)) {
            longUrl = longUrlRequest;
        } else if (isValidUrl(HTTPS + longUrlRequest)) {
            longUrl = HTTPS + longUrlRequest;
        } else {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(BAD_REQUEST.code)
                    .withBody("Invalid URL")
                    .build();
        }

        final byte[] longUrlBytes = longUrl.getBytes();
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGO);
            messageDigest.update(longUrlBytes);
            int appends = 0;

            while(true) {
                // Generate short URL
                final String shortUrl = digestToShortUrl(messageDigest.digest()).substring(0, TARGET_LENGTH);

                // Check if shortUrl already exists in db
                // 1. If not, put new URL
                // 2. If so, check if its longUrl equals request longUrl
                //    i. If so, do nothing (success)
                //    ii. If not, then there is a hash collision (extremely unlikely). Append string and start over.
                final Optional<String> dbUrl = DDB.getUrl(shortUrl);
                if (dbUrl.isEmpty()) {
                    DDB.putUrl(shortUrl, longUrl);
                    return getSuccessResponse(shortUrl);
                } else {
                    if (dbUrl.get().equals(longUrl)) {
                        return getSuccessResponse(shortUrl);
                    }

                    // Append string to digest and loop.
                    messageDigest.update(longUrlBytes);
                    appends++;
                    for (int i = 0; i < appends; i++) {
                        messageDigest.update(APPEND_STRING);
                    }
                }
            }

        } catch (final NoSuchAlgorithmException exception) {
            throw new RuntimeException(String.format("MessageDigest could not find hash algo: %s", HASH_ALGO), exception);
        }
    }

    private static boolean isValidUrl(final String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch(final Exception exception) {
            return false;
        }
    }

    private static String digestToShortUrl(final byte[] digest) {
        BigInteger digestInt = new BigInteger(1, digest);
        final StringBuilder stringBuilder = new StringBuilder();
        do {
            final BigInteger[] divWithRem = digestInt.divideAndRemainder(TARGET_BASE);
            digestInt = divWithRem[0];
            stringBuilder.append(SHORT_URL_CHAR_SET.charAt(divWithRem[1].intValue()));
        } while (digestInt.compareTo(BigInteger.ZERO) > 0);
        return stringBuilder.reverse().toString();
    }

    private static APIGatewayV2HTTPResponse getSuccessResponse(final String shortUrl) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(SUCCESS.code)
                .withHeaders(JSON_HEADER)
                .withBody(SerializationUtils.toJson(SHORT_URL_KEY, shortUrl))
                .build();
    }

}
