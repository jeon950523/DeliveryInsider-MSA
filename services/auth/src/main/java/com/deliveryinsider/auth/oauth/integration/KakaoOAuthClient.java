package com.deliveryinsider.auth.oauth.integration;

import com.deliveryinsider.auth.global.config.KakaoOAuthProperties;
import com.deliveryinsider.auth.global.error.AuthErrorCode;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.oauth.model.KakaoUserProfile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class KakaoOAuthClient {

    private final RestClient kakaoAuthRestClient;
    private final RestClient kakaoApiRestClient;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(
        @Qualifier("kakaoAuthRestClient")
        RestClient kakaoAuthRestClient,
        @Qualifier("kakaoApiRestClient")
        RestClient kakaoApiRestClient,
        KakaoOAuthProperties properties
    ) {
        this.kakaoAuthRestClient = kakaoAuthRestClient;
        this.kakaoApiRestClient = kakaoApiRestClient;
        this.properties = properties;
    }

    public KakaoUserProfile fetchUserByAuthorizationCode(
        String code
    ) {
        try {
            String accessToken =
                requestAccessToken(code);

            return requestUserProfile(accessToken);

        } catch (BusinessException e) {
            throw e;

        } catch (RestClientException | ClassCastException e) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_PROVIDER_ERROR,
                e
            );
        }
    }

    private String requestAccessToken(String code) {
        MultiValueMap<String, String> form =
            new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.restApiKey());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", code);
        form.add("client_secret", properties.clientSecret());

        Map<?, ?> response =
            kakaoAuthRestClient
                .post()
                .uri("/oauth/token")
                .contentType(
                    MediaType.APPLICATION_FORM_URLENCODED
                )
                .body(form)
                .retrieve()
                .body(Map.class);

        String accessToken =
            response == null
                ? null
                : asString(response.get("access_token"));

        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_PROVIDER_ERROR
            );
        }

        return accessToken;
    }

    private KakaoUserProfile requestUserProfile(
        String accessToken
    ) {
        Map<?, ?> response =
            kakaoApiRestClient
                .get()
                .uri("/v2/user/me")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + accessToken
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_PROVIDER_ERROR
            );
        }

        String providerUserId =
            asString(response.get("id"));

        Object accountValue =
            response.get("kakao_account");

        if (
            !StringUtils.hasText(providerUserId)
            || !(accountValue instanceof Map<?, ?> account)
        ) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_PROVIDER_ERROR
            );
        }

        return new KakaoUserProfile(
            providerUserId,
            asString(account.get("email")),
            Boolean.TRUE.equals(
                account.get("is_email_valid")
            ),
            Boolean.TRUE.equals(
                account.get("is_email_verified")
            )
        );
    }

    private String asString(Object value) {
        return value == null
            ? null
            : String.valueOf(value);
    }
}
