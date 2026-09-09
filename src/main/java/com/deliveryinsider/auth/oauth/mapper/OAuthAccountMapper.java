package com.deliveryinsider.auth.oauth.mapper;

import com.deliveryinsider.auth.oauth.entity.OAuthAccountEntity;
import com.deliveryinsider.auth.oauth.entity.OAuthProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface OAuthAccountMapper {

    Optional<OAuthAccountEntity> findByProviderAndProviderUserId(
        @Param("provider") OAuthProvider provider,
        @Param("providerUserId") String providerUserId
    );

    Optional<OAuthAccountEntity> findByUserIdAndProvider(
        @Param("userId") Long userId,
        @Param("provider") OAuthProvider provider
    );

    int insert(OAuthAccountEntity account);
}
