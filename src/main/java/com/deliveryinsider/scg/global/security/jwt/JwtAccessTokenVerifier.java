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
        try {
            Claims claims = jwtParser
                .parseSignedClaims(accessToken)
                .getPayload();

            return Optional.ofNullable(claims.getSubject())
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .orElseThrow(InvalidAccessTokenException::new);

        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidAccessTokenException(e);
        }
    }
}
