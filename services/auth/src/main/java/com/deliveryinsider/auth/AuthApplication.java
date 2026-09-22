package com.deliveryinsider.auth;

import com.deliveryinsider.auth.global.config.JwtProperties;
import com.deliveryinsider.auth.global.config.KakaoOAuthProperties;
import com.deliveryinsider.auth.global.config.PhoneVerificationProperties;
import com.deliveryinsider.auth.global.config.SolapiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({
    JwtProperties.class,
    KakaoOAuthProperties.class,
    PhoneVerificationProperties.class,
    SolapiProperties.class
})
@SpringBootApplication
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
