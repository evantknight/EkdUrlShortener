package dev.evanknight.urlshortener.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WriteLambdaRequest {
    private String longUrl;
}
