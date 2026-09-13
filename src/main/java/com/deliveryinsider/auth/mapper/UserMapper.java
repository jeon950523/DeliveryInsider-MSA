package com.deliveryinsider.auth.mapper;

import com.deliveryinsider.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Mapper
public interface UserMapper {
    int insert(UserEntity user);

    Optional<UserEntity> findByEmail(@Param("email")String email);
    Optional<UserEntity> findById(@Param("id")Long id);
    Optional<UserEntity> findByPhoneNumber(
        @Param("phoneNumber") String phoneNumber
    );
    int updateVerifiedPhone(
        @Param("id") Long id,
        @Param("phoneNumber") String phoneNumber,
        @Param("phoneVerifiedAt") LocalDateTime phoneVerifiedAt
    );
    int updateStatus(@Param("id")Long id, @Param("status")String status);
    int updatePasswordHash(
        @Param("id") Long id,
        @Param("passwordHash") String passwordHash
    );

    long countAll();
    long countByStatus(@Param("status") String status);
    long countPhoneVerified();
    long countByRole(@Param("role") String role);
    List<UserEntity> findAdminPage(
        @Param("limit") int limit,
        @Param("offset") int offset
    );
}
