package com.deliveryinsider.simulator.domain.baemin.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WebhookSignerTest {

    private final WebhookSigner signer = new WebhookSigner();

    @Test
    void sameInputProducesSameSignature() {
        String first = signer.sign(
            "test-secret",
            1_725_000_000L,
            "{\"sourceEventId\":\"evt-1\"}"
        );

        String second = signer.sign(
            "test-secret",
            1_725_000_000L,
            "{\"sourceEventId\":\"evt-1\"}"
        );

        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void changedRawBodyChangesSignature() {
        String first = signer.sign(
            "test-secret",
            1_725_000_000L,
            "{\"sourceEventId\":\"evt-1\"}"
        );

        String second = signer.sign(
            "test-secret",
            1_725_000_000L,
            "{\"sourceEventId\":\"evt-2\"}"
        );

        assertNotEquals(first, second);
    }
}
