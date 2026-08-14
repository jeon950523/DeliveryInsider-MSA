package com.deliveryinsider.auth.mapper;

import com.deliveryinsider.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    int insert(UserEntity user);

    Optional<UserEntity> findByEmail(@Param("email")String email);
    Optional<UserEntity> findById(@Param("id")Long id);
    int updateStatus(@Param("id")Long id, @Param("status")String status);
}
