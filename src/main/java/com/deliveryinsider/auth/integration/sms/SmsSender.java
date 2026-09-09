package com.deliveryinsider.auth.integration.sms;

public interface SmsSender {
    void sendVerificationCode(String phoneNumber, String code);
}
