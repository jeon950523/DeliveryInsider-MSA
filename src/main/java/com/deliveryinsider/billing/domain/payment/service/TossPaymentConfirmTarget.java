package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;

public record TossPaymentConfirmTarget(

    PaymentEntity payment

) {
}
