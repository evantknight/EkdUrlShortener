package dev.evanknight.urlshortener.service.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import dev.evanknight.urlshortener.service.constant.ResponseConstants;
import dev.evanknight.urlshortener.service.proxy.DynamoDb;
import dev.evanknight.urlshortener.service.util.FileUtils;

import java.util.Map;
import java.util.Optional;

public class ReadLambda implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final String INDEX_HTML = FileUtils.readFile(FileUtils.INDEX_PATH);
    private static final String NOT_FOUND_HTML = FileUtils.readFile(FileUtils.NOT_FOUND_PATH);
    private static final DynamoDb DDB = new DynamoDb();

    // SnapStart Warmup
    static {
        DDB.getUrl("warmup");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(final APIGatewayV2HTTPEvent request, final Context context) {
        final String rawPath = request.getRawPath().trim();

        // Check if request is for index.
        if ("/".equals(rawPath) || rawPath.isBlank()) {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(ResponseConstants.HttpStatus.SUCCESS.code)
                    .withHeaders(Map.of(ResponseConstants.CONTENT_TYPE_HEADER, ResponseConstants.HTML_CONTENT))
                    .withBody(INDEX_HTML)
                    .build();
        }

        final String shortUrl;
        if (rawPath.startsWith("/")) {
            shortUrl = rawPath.substring(1);
        } else {
            shortUrl = rawPath;
        }

        final Optional<String> longUrlOptional = DDB.getUrl(shortUrl);
        if (longUrlOptional.isPresent()) {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(ResponseConstants.HttpStatus.FOUND.code)
                    .withHeaders(Map.of(ResponseConstants.LOCATION_HEADER, longUrlOptional.get()))
                    .build();
        } else {
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(ResponseConstants.HttpStatus.NOT_FOUND.code)
                    .withHeaders(Map.of(ResponseConstants.CONTENT_TYPE_HEADER, ResponseConstants.HTML_CONTENT))
                    .withBody(NOT_FOUND_HTML)
                    .build();
        }
    }
}
