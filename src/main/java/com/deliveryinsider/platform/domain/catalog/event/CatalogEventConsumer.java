package com.deliveryinsider.platform.domain.catalog.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {
    private final CatalogProjectionHandler handler;
    private final JsonMapper json;
    public void receive(String body) {
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
            handler.apply(json.readValue(body, CatalogEvent.class), hash);
        } catch (JacksonException e) {
            throw new InvalidCatalogEventException("Malformed catalog event JSON", e);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
