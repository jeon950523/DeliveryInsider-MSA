package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentConfirmRequest;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentFailRequest;
import com.deliveryinsider.billing.domain.payment.response.PaymentResponse;
import com.deliveryinsider.billing.domain.payment.response.TossPaymentPrepareResponse;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResult;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResultStatus;
import com.deliveryinsider.billing.integration.payment.TossPaymentClient;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TossPaymentService {

    private final CurrentStoreClient currentStoreClient;

    private final InitialPaymentTransactionService initialPaymentTransactionService;

    private final TossPaymentConfirmTransactionService confirmTransactionService;

    private final TossPaymentClient tossPaymentClient;

    /**
     * Toss 결제창을 열기 전에
     * Billing DB에 REQUESTED Payment를 먼저 생성한다.
     */
    public TossPaymentPrepareResponse prepare(
        Long userId
    ) {
        var store =
            currentStoreClient.findByUserId(
                userId
            );

        PaymentEntity payment =
            initialPaymentTransactionService.prepareToss(
                store.storeId()
            );

        return new TossPaymentPrepareResponse(
            payment.getId(),
            payment.getSubscriptionId(),
            payment.getPaymentOrderId(),
            "DeliveryInsider 월 구독",
            payment.getAmount()
        );
    }

    /**
     * Toss 결제 인증이 끝난 뒤
     * Frontend가 전달한 paymentKey / orderId / amount를 검증하고
     * 실제 Toss 승인 API를 호출한다.
     */
    public PaymentResponse confirm(
        Long userId,
        TossPaymentConfirmRequest request
    ) {
        var store =
            currentStoreClient.findByUserId(
                userId
            );

        TossPaymentConfirmTarget target =
            confirmTransactionService.validate(
                store.storeId(),
                request
            );

        PaymentEntity payment =
            target.payment();

        PaymentProviderResult result =
            tossPaymentClient.confirm(
                request.paymentKey(),
                payment.getPaymentOrderId(),
                payment.getAmount(),
                payment.getIdempotencyKey()
            );

        return switch (result.status()) {

            case SUCCEEDED ->
                PaymentResponse.from(
                    initialPaymentTransactionService.succeed(
                        payment.getId(),
                        result
                    )
                );

            case FAILED ->
                PaymentResponse.from(
                    initialPaymentTransactionService.fail(
                        payment.getId(),
                        result
                    )
                );

            case UNKNOWN -> handleUnknown(
                payment,
                result
            );
        };
    }

    private PaymentResponse handleUnknown(
        PaymentEntity payment,
        PaymentProviderResult result
    ) {
        /*
         * 이미 UNKNOWN인 Payment를 다시 UNKNOWN으로 바꿀 필요는 없다.
         *
         * 예:
         * 1차 Toss 승인 Timeout → UNKNOWN
         * 사용자가 confirm을 다시 호출
         * Toss 조회/승인 결과가 여전히 불명확
         *
         * 이 경우 기존 UNKNOWN Payment를 그대로 반환한다.
         */
        if (payment.getStatus()
            == PaymentStatus.UNKNOWN) {

            return PaymentResponse.from(
                payment
            );
        }

        /*
         * REQUESTED 상태에서 Toss 승인 결과를 알 수 없게 된 경우.
         */
        PaymentEntity unknownPayment =
            initialPaymentTransactionService.unknown(
                payment.getId(),
                result.failureMessage()
            );

        return PaymentResponse.from(
            unknownPayment
        );
    }
    public PaymentResponse fail(
        Long userId,
        TossPaymentFailRequest request
    ) {
        var store =
            currentStoreClient.findByUserId(userId);

        PaymentEntity payment =
            confirmTransactionService.validateFailure(
                store.storeId(),
                request.orderId()
            );

        PaymentProviderResult result =
            new PaymentProviderResult(
                PaymentProviderResultStatus.FAILED,
                null,
                null,
                null,
                null,
                request.code(),
                request.message()
            );

        return PaymentResponse.from(
            initialPaymentTransactionService.fail(
                payment.getId(),
                result
            )
        );
    }
}
