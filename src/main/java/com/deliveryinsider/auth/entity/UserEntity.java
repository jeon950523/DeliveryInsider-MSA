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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public UserEntity(
        String email,
        String passwordHash,
        UserStatus status
    ){
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
    }
}
