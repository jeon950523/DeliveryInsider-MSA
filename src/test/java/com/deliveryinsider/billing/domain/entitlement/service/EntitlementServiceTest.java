package com.deliveryinsider.billing.domain.entitlement.service;

import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.entitlement.model.PremiumFeatureCode;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntitlementServiceTest {

    private final SubscriptionMapper subscriptionMapper =
        mock(SubscriptionMapper.class);

    private final EntitlementService service =
        new EntitlementService(
            subscriptionMapper
        );

    @Test
    void subscription이_없으면_구독필요로_판정한다() {
        when(
            subscriptionMapper.findCurrentByStoreId(3L)
        ).thenReturn(
            Optional.empty()
        );

        var result =
            service.find(3L);

        assertThat(result.entitled())
            .isFalse();

        assertThat(result.accessCode())
            .isEqualTo(
                EntitlementService.SUBSCRIPTION_REQUIRED
            );
    }

    @Test
    void subscription이_없으면_두_프리미엄기능은_모두_잠긴다() {
        when(
            subscriptionMapper.findCurrentByStoreId(3L)
        ).thenReturn(Optional.empty());

        for (PremiumFeatureCode featureCode : PremiumFeatureCode.values()) {
            var result = service.findFeature(3L, featureCode);

            assertThat(result.featureCode()).isEqualTo(featureCode);
            assertThat(result.entitled()).isFalse();
            assertThat(result.subscriptionStatus()).isNull();
        }
    }

    @Test
    void active이고_기간이_남아있으면_entitled다() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 9,
                0, 0
            );

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.ACTIVE,
                now.plusDays(1)
            );

        assertThat(
            service.isEntitled(
                subscription,
                now
            )
        ).isTrue();
    }

    @Test
    void active라도_기간이_끝났으면_scheduler전이라도_차단한다() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 9,
                0, 0
            );

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.ACTIVE,
                now
            );

        assertThat(
            service.isEntitled(
                subscription,
                now
            )
        ).isFalse();
    }

    @Test
    void canceled는_현재기간_종료전까지만_entitled다() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 9,
                0, 0
            );

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.CANCELED,
                now.plusSeconds(1)
            );

        assertThat(
            service.isEntitled(
                subscription,
                now
            )
        ).isTrue();

        subscription =
            subscription(
                SubscriptionStatus.CANCELED,
                now
            );

        assertThat(
            service.isEntitled(
                subscription,
                now
            )
        ).isFalse();
    }

    @Test
    void pending과_pastDue는_entitled가_아니다() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 9,
                0, 0
            );

        for (SubscriptionStatus status
            : new SubscriptionStatus[]{
                SubscriptionStatus.PENDING,
                SubscriptionStatus.PAST_DUE
            }) {

            SubscriptionEntity subscription =
                subscription(
                    status,
                    now.plusDays(1)
                );

            assertThat(
                service.isEntitled(
                    subscription,
                    now
                )
            ).isFalse();
        }
    }

    private SubscriptionEntity subscription(
        SubscriptionStatus status,
        LocalDateTime periodEnd
    ) {
        SubscriptionEntity subscription =
            new SubscriptionEntity();

        subscription.setStoreId(3L);
        subscription.setStatus(status);
        subscription.setCurrentPeriodEnd(periodEnd);

        return subscription;
    }
}
