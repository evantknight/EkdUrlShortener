package dev.evanknight.urlshortener.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WriteLambdaResponse {
    private String shortUrl;
}
