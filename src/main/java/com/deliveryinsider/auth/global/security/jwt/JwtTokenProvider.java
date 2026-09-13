package com.deliveryinsider.auth.global.security.jwt;

import com.deliveryinsider.auth.global.config.JwtProperties;
import com.deliveryinsider.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private static final String TOKEN_TYPE_CLAIM="tokenType";
    private static final String ACCESS_TOKEN_TYPE="ACCESS";
    private static final String REFRESH_TOKEN_TYPE="REFRESH";
    private static final String ROLE_CLAIM="role";

    private final JwtParser accessTokenParser;
    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final JwtParser refreshTokenParser;

    public JwtTokenProvider(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        this.secretKey = createSecretKey(jwtProperties.secret());

        this.accessTokenParser = createTokenParser(ACCESS_TOKEN_TYPE);
        this.refreshTokenParser = createTokenParser(REFRESH_TOKEN_TYPE);


    }
    public String createAccessToken(Long userId){
        return createAccessToken(userId, UserRole.USER);
    }
    public String createAccessToken(Long userId, UserRole role){
        return createToken(userId,jwtProperties.accessTokenExpiryMs(),ACCESS_TOKEN_TYPE, role);
    }
    public String createRefreshToken(Long userId){
        return createToken(userId,jwtProperties.refreshTokenExpiryMs(),REFRESH_TOKEN_TYPE, null);
    }


    public Claims parseAccessToken(String accessToken){
        return accessTokenParser
            .parseSignedClaims(accessToken)
            .getPayload();
    }
    public Claims parseRefreshToken(String refreshToken){
        return refreshTokenParser
            .parseSignedClaims(refreshToken)
            .getPayload();
    }

    public String createToken(Long userId, long expiryMs, String tokenType){
        return createToken(userId, expiryMs, tokenType, null);
    }
    private String createToken(Long userId, long expiryMs, String tokenType, UserRole role){
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expiryMs);
        if (userId == null){
            throw new IllegalArgumentException("User id cannot be null");
        }
        var builder = Jwts.builder()
            .issuer(jwtProperties.issuer())
            .subject(String.valueOf(userId))
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .claim(TOKEN_TYPE_CLAIM, tokenType);
        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }
        return builder.signWith(secretKey).compact();
    }
    private SecretKey createSecretKey(String encodedSecret){
        byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    private JwtParser createTokenParser(String tokenType){
        return Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(jwtProperties.issuer())
            .require(TOKEN_TYPE_CLAIM, tokenType)
            .build();
    }

}
