package dev.evanknight.urlshortener.service.proxy;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static dev.evanknight.urlshortener.constants.DynamoDb.LONG_URL_NAME;
import static dev.evanknight.urlshortener.constants.DynamoDb.PRIMARY_KEY;
import static dev.evanknight.urlshortener.constants.DynamoDb.TTL_NAME;
import static dev.evanknight.urlshortener.constants.Lambda.DDB_TABLE_NAME;

public class DynamoDb {
    private static final long TTL_MINS = 10;
    private static final String TABLE_NAME = System.getenv(DDB_TABLE_NAME);

    private final DynamoDbClient CLIENT = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build();


    public Optional<String> getUrl(final String shortUrl) {
        // Build request
        final Map<String, AttributeValue> key = Map.of(
                PRIMARY_KEY, AttributeValue.fromS(shortUrl)
        );
        final GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(key)
                .build();

        // Call DynamoDB
        final GetItemResponse response = CLIENT.getItem(request);

        // Process response
        if (!response.hasItem()) {
            return Optional.empty();
        }
        final Map<String, AttributeValue> item = response.item();
        return Optional.of(item.get(LONG_URL_NAME).s());
    }

    public void putUrl(final String shortUrl, final String longUrl) {
        // Generate TTL
        final Instant ttlInstant = Instant.now().plus(TTL_MINS, ChronoUnit.MINUTES);

        // Build request
        final Map<String, AttributeValue> item = Map.of(
                PRIMARY_KEY, AttributeValue.fromS(shortUrl),
                LONG_URL_NAME, AttributeValue.fromS(longUrl),
                TTL_NAME, AttributeValue.fromN(String.valueOf(ttlInstant.getEpochSecond()))
        );
        final PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        // Call DynamoDB
        CLIENT.putItem(request);
    }

}
