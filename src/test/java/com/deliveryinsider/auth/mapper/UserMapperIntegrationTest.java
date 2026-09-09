package com.deliveryinsider.auth.mapper;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserMapperIntegrationTest {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    UserMapperIntegrationTest(
        UserMapper userMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void insertAndFindUserByEmail() {
        String email =
            "owner-" + UUID.randomUUID() + "@test.com";

        String rawPassword = "Test1234!";

        UserEntity user = UserEntity.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .status(UserStatus.ACTIVE)
            .build();

        int inserted = userMapper.insert(user);

        UserEntity found = userMapper.findByEmail(email)
            .orElseThrow();

        assertEquals(1, inserted);
        assertNotNull(user.getId());
        assertEquals(user.getId(), found.getId());
        assertEquals(UserStatus.ACTIVE, found.getStatus());
        assertTrue(
            passwordEncoder.matches(
                rawPassword,
                found.getPasswordHash()
            )
        );
    }

    @Test
    void updateUserStatus() {
        String email =
            "pending-" + UUID.randomUUID() + "@test.com";

        UserEntity user = UserEntity.builder()
            .email(email)
            .passwordHash(
                passwordEncoder.encode("Test1234!")
            )
            .status(UserStatus.PENDING_ONBOARDING)
            .build();

        userMapper.insert(user);

        int updated = userMapper.updateStatus(
            user.getId(),
            UserStatus.ACTIVE.name()
        );

        UserEntity found = userMapper.findById(user.getId())
            .orElseThrow();

        assertEquals(1, updated);
        assertEquals(UserStatus.ACTIVE, found.getStatus());
    }
    @Test
    void updateVerifiedPhoneAndRejectDuplicatePhone() {
        UserEntity first = UserEntity.builder()
            .email("phone-first-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("Test1234!"))
            .status(UserStatus.ACTIVE)
            .build();
        UserEntity second = UserEntity.builder()
            .email("phone-second-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode("Test1234!"))
            .status(UserStatus.ACTIVE)
            .build();

        userMapper.insert(first);
        userMapper.insert(second);

        LocalDateTime verifiedAt = LocalDateTime.now();
        String phoneNumber = "01012345678";

        assertEquals(
            1,
            userMapper.updateVerifiedPhone(
                first.getId(),
                phoneNumber,
                verifiedAt
            )
        );

        UserEntity found = userMapper.findById(first.getId())
            .orElseThrow();

        assertEquals(phoneNumber, found.getPhoneNumber());
        assertNotNull(found.getPhoneVerifiedAt());

        assertThrows(
            DuplicateKeyException.class,
            () -> userMapper.updateVerifiedPhone(
                second.getId(),
                phoneNumber,
                verifiedAt
            )
        );
    }

}
