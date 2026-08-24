package com.deliveryinsider.scg.global.filter;

import com.deliveryinsider.scg.global.error.GatewayErrorResponse;
import com.deliveryinsider.scg.global.security.jwt.InvalidAccessTokenException;
import com.deliveryinsider.scg.global.security.jwt.JwtAccessTokenVerifier;
import com.deliveryinsider.scg.global.security.jwt.JwtProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;


import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthenticationWebFilter implements WebFilter {

    public static final String USER_ID_HEADER = "X-User-Id";

    private static final String INTERNAL_API_KEY_HEADER =
        "X-Internal-Api-Key";

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/reissue-token",
        "/api/auth/logout"
    );

    private final JwtAccessTokenVerifier jwtAccessTokenVerifier;
    private final JwtProperties jwtProperties;
    private final JsonMapper jsonMapper;

    @Override
    public Mono<Void> filter(
        ServerWebExchange exchange,
        WebFilterChain chain
    ) {
        ServerWebExchange sanitizedExchange =
            sanitizeTrustedHeaders(exchange);

        ServerHttpRequest request =
            sanitizedExchange.getRequest();

        String path =
            request.getURI().getPath();

        if (HttpMethod.OPTIONS.equals(request.getMethod())
            || !requiresAuthentication(path)
            || PUBLIC_AUTH_PATHS.contains(path)) {

            return chain.filter(sanitizedExchange);
        }

        try {
            String accessToken =
                resolveAccessToken(request)
                    .orElseThrow(
                        InvalidAccessTokenException::new
                    );

            Long userId =
                jwtAccessTokenVerifier
                    .verifyAndExtractUserId(accessToken);

            ServerHttpRequest authenticatedRequest =
                request.mutate()
                    .headers(headers ->
                        headers.set(
                            USER_ID_HEADER,
                            String.valueOf(userId)
                        )
                    )
                    .build();

            return chain.filter(
                sanitizedExchange.mutate()
                    .request(authenticatedRequest)
                    .build()
            );

        } catch (InvalidAccessTokenException e) {
            return unauthorized(sanitizedExchange);
        }
    }

    private ServerWebExchange sanitizeTrustedHeaders(
        ServerWebExchange exchange
    ) {
        return exchange.mutate()
            .request(builder ->
                builder.headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(INTERNAL_API_KEY_HEADER);
                })
            )
            .build();
    }

    private boolean requiresAuthentication(String path) {
        return path.startsWith("/api/");
    }

    private Optional<String> resolveAccessToken(
        ServerHttpRequest request
    ) {
        String authorization = request
            .getHeaders()
            .getFirst(jwtProperties.headerKey());

        String prefix =
            jwtProperties.scheme() + " ";

        return Optional.ofNullable(authorization)
            .filter(StringUtils::hasText)
            .filter(value ->
                value.regionMatches(
                    true,
                    0,
                    prefix,
                    0,
                    prefix.length()
                )
            )
            .map(value ->
                value.substring(prefix.length()).trim()
            )
            .filter(StringUtils::hasText);
    }

    private Mono<Void> unauthorized(
        ServerWebExchange exchange
    ) {
        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders()
            .setContentType(MediaType.APPLICATION_JSON);

        response.getHeaders().set(
            HttpHeaders.WWW_AUTHENTICATE,
            jwtProperties.scheme()
        );

        GatewayErrorResponse<Void> errorResponse =
            GatewayErrorResponse.of(
                "AUTH-004",
                "유효하지 않은 토큰입니다."
            );

        try {
            byte[] body =
                jsonMapper.writeValueAsBytes(errorResponse);

            return response.writeWith(
                Mono.just(
                    response.bufferFactory().wrap(body)
                )
            );

        } catch (JacksonException e) {
            return response.setComplete();
        }
    }
}
