package com.deliveryinsider.auth.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {
    private Long id;
    private String email;
    private String passwordHash;
    private UserStatus status;
    private UserRole role;
    private String phoneNumber;
    private LocalDateTime phoneVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public UserEntity(
        String email,
        String passwordHash,
        UserStatus status,
        UserRole role
    ){
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.role = role == null ? UserRole.USER : role;
    }
}
