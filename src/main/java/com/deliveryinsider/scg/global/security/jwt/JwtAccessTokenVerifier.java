package com.deliveryinsider.scg.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class JwtAccessTokenVerifier {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String ROLE_CLAIM = "role";
    private static final String DEFAULT_ROLE = "USER";

    private final JwtParser jwtParser;

    public JwtAccessTokenVerifier(JwtProperties jwtProperties) {
        SecretKey secretKey = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(jwtProperties.secret())
        );

        this.jwtParser = Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(jwtProperties.issuer())
            .require(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .build();
    }

    public Long verifyAndExtractUserId(String accessToken) {
        return verify(accessToken).userId();
    }

    public VerifiedPrincipal verify(String accessToken) {
        try {
            Claims claims = jwtParser
                .parseSignedClaims(accessToken)
                .getPayload();

            Long userId = Optional.ofNullable(claims.getSubject())
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .orElseThrow(InvalidAccessTokenException::new);

            String role = Optional.ofNullable(
                    claims.get(ROLE_CLAIM, String.class)
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(value -> value.equals("USER") || value.equals("ADMIN"))
                .orElse(DEFAULT_ROLE);

            return new VerifiedPrincipal(userId, role);

        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidAccessTokenException(e);
        }
    }

    public record VerifiedPrincipal(Long userId, String role) {
    }
}
