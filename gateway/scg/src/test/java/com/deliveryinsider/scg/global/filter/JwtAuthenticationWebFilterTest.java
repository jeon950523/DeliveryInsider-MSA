package com.deliveryinsider.scg.global.filter;

import com.deliveryinsider.scg.global.security.jwt.JwtAccessTokenVerifier;
import com.deliveryinsider.scg.global.security.jwt.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationWebFilterTest {

    private static final String TEST_SECRET =
        "VGhpcy1pcy1hLXRlc3Qta2V5LXdpdGgtMzItYnl0ZXMhIQ==";

    private final JwtProperties jwtProperties =
        new JwtProperties(
            "deliveryinsider",
            "Authorization",
            "Bearer",
            TEST_SECRET
        );

    private final JwtAuthenticationWebFilter filter =
        new JwtAuthenticationWebFilter(
            new JwtAccessTokenVerifier(jwtProperties),
            jwtProperties,
            JsonMapper.builder().build()
        );

    @Test
    void validAccessTokenReplacesSpoofedUserIdHeader() {
        String accessToken =
            createToken("11", "ACCESS");

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/stores/me")
                    .header(
                        "Authorization",
                        "Bearer " + accessToken
                    )
                    .header("X-User-Id", "999")
                    .build()
            );

        AtomicReference<String> forwardedUserId =
            new AtomicReference<>();

        WebFilterChain chain = currentExchange -> {
            forwardedUserId.set(
                currentExchange
                    .getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id")
            );

            return Mono.empty();
        };

        StepVerifier.create(
            filter.filter(exchange, chain)
        ).verifyComplete();

        assertEquals("11", forwardedUserId.get());
    }

    @Test
    void validAccessTokenReplacesSpoofedRoleHeader() {
        String accessToken = createToken("11", "ACCESS", "USER");
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/stores/me")
                .header("Authorization", "Bearer " + accessToken)
                .header("X-User-Role", "ADMIN")
                .build()
        );
        AtomicReference<String> forwardedRole = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, currentExchange -> {
            forwardedRole.set(currentExchange.getRequest().getHeaders().getFirst("X-User-Role"));
            return Mono.empty();
        })).verifyComplete();

        assertEquals("USER", forwardedRole.get());
    }

    @Test
    void userTokenCannotCallAdminApi() {
        String accessToken = createToken("11", "ACCESS", "USER");
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/admin/store/stores")
                .header("Authorization", "Bearer " + accessToken)
                .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, currentExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void adminTokenCanCallAdminApi() {
        String accessToken = createToken("11", "ACCESS", "ADMIN");
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/admin/auth/users")
                .header("Authorization", "Bearer " + accessToken)
                .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, currentExchange -> {
            chainCalled.set(true);
            assertEquals("ADMIN", currentExchange.getRequest().getHeaders().getFirst("X-User-Role"));
            return Mono.empty();
        })).verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void missingAccessTokenReturnsUnauthorized() {
        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/stores/me")
                    .build()
            );

        AtomicBoolean chainCalled =
            new AtomicBoolean(false);

        WebFilterChain chain = currentExchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(
            filter.filter(exchange, chain)
        ).verifyComplete();

        assertFalse(chainCalled.get());
        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exchange.getResponse().getStatusCode()
        );
    }

    @Test
    void refreshTokenCannotAuthenticateApiRequest() {
        String refreshToken =
            createToken("11", "REFRESH");

        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/api/stores/me")
                    .header(
                        "Authorization",
                        "Bearer " + refreshToken
                    )
                    .build()
            );

        StepVerifier.create(
            filter.filter(
                exchange,
                currentExchange -> Mono.empty()
            )
        ).verifyComplete();

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exchange.getResponse().getStatusCode()
        );
    }

    @Test
    void publicLoginRemovesSpoofedUserIdHeader() {
        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .post("/api/auth/login")
                    .header("X-User-Id", "999")
                    .build()
            );

        AtomicReference<String> forwardedUserId =
            new AtomicReference<>();

        WebFilterChain chain = currentExchange -> {
            forwardedUserId.set(
                currentExchange
                    .getRequest()
                    .getHeaders()
                    .getFirst("X-User-Id")
            );

            return Mono.empty();
        };

        StepVerifier.create(
            filter.filter(exchange, chain)
        ).verifyComplete();

        assertNull(forwardedUserId.get());
    }



    @Test
    void kakaoOAuthCallbackIsPublicAndTrustedHeadersAreRemoved() {
        MockServerWebExchange exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get(
                        "/api/auth/oauth2/callback/kakao"
                    )
                    .header(
                        "X-User-Id",
                        "999"
                    )
                    .build()
            );

        AtomicReference<String> forwardedUserId =
            new AtomicReference<>();

        WebFilterChain chain =
            currentExchange -> {
                forwardedUserId.set(
                    currentExchange
                        .getRequest()
                        .getHeaders()
                        .getFirst("X-User-Id")
                );

                return Mono.empty();
            };

        StepVerifier.create(
            filter.filter(
                exchange,
                chain
            )
        ).verifyComplete();

        assertNull(
            forwardedUserId.get()
        );
    }

    private String createToken(
        String subject,
        String tokenType
    ) {
        return createToken(subject, tokenType, null);
    }

    private String createToken(
        String subject,
        String tokenType,
        String role
    ) {
        SecretKey secretKey =
            Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(TEST_SECRET)
            );

        Instant issuedAt = Instant.now();

        var builder = Jwts.builder()
            .issuer("deliveryinsider")
            .subject(subject)
            .issuedAt(Date.from(issuedAt))
            .expiration(
                Date.from(
                    issuedAt.plusSeconds(300)
                )
            )
            .claim("tokenType", tokenType);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.signWith(secretKey).compact();
    }
}
