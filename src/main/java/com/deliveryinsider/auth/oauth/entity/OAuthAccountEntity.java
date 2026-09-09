package com.deliveryinsider.auth.oauth.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OAuthAccountEntity {

    private Long id;
    private Long userId;
    private OAuthProvider provider;
    private String providerUserId;
    private String providerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public OAuthAccountEntity(
        Long userId,
        OAuthProvider provider,
        String providerUserId,
        String providerEmail
    ) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
    }
}
