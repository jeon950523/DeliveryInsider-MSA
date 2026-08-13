package com.deliveryinsider.global.security.jwt;

import com.deliveryinsider.global.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.time.Instant;

@Component
public class JwtTokenProvider {
    private static final String TOKEN_TYPE_CLAIM="tokenType";
    private static final String ACCESS_TOKEN_TYPE="ACCESS";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey(jwtProperties.secret());
    }
    public String createAccessToken(Long userId){
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtProperties.accessTokenExpiryMs());

        return Jwts.builder()
            .issuer(jwtProperties.issuer())
            .subject(String.valueOf(userId))
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .signWith(secretKey)
            .compact();
    }
    private SecretKey createSecretKey(String encodedSecret){
        byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
