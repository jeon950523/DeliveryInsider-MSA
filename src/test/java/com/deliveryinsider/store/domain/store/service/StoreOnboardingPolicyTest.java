package com.deliveryinsider.store.domain.store.service;

import com.deliveryinsider.store.domain.catalog.CatalogEventWriter;
import com.deliveryinsider.store.domain.store.entity.BusinessVerification;
import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.BusinessVerificationMapper;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.domain.store.request.BusinessVerificationRequest;
import com.deliveryinsider.store.domain.store.request.StoreCreateRequest;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import com.deliveryinsider.store.integration.nts.NtsBusinessClient;
import com.deliveryinsider.store.integration.nts.NtsBusinessVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreOnboardingPolicyTest {

    @Mock
    private StoreMapper storeMapper;
    @Mock
    private BusinessVerificationMapper businessVerificationMapper;
    @Mock
    private NtsBusinessClient ntsBusinessClient;
    @Mock
    private CatalogEventWriter catalogEventWriter;

    private StoreService storeService;
    private BusinessVerificationService verificationService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(
                storeMapper,
                catalogEventWriter,
                businessVerificationMapper
        );
        verificationService = new BusinessVerificationService(
                ntsBusinessClient,
                businessVerificationMapper,
                storeMapper
        );
        ReflectionTestUtils.setField(
                verificationService,
                "verificationTtlMinutes",
                30L
        );
    }

    @Test
    void 활성_매장이_있는_사용자는_매장을_다시_생성할_수_없다() {
        when(storeMapper.findByUserId(19L)).thenReturn(
                Store.builder().id(5L).userId(19L).build()
        );

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> storeService.create(19L, createRequest("verification-id"))
        );

        assertEquals(StoreErrorCode.STORE_ALREADY_EXISTS, error.errorCode());
        verify(businessVerificationMapper, never()).findByIdForUpdate(anyString());
    }

    @Test
    void 다른_사용자가_이미_등록한_활성_사업자번호로_매장을_생성할_수_없다() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        BusinessVerification verification = BusinessVerification.builder()
                .id("verification-id")
                .userId(20L)
                .businessRegistrationNumber("1234567890")
                .businessStatusCode("01")
                .expiresAt(now.plusMinutes(10))
                .build();

        when(storeMapper.findByUserId(20L)).thenReturn(null);
        when(businessVerificationMapper.findByIdForUpdate("verification-id"))
                .thenReturn(verification);
        when(storeMapper.existsByBusinessRegistrationNumber("1234567890"))
                .thenReturn(true);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> storeService.create(20L, createRequest("verification-id"))
        );

        assertEquals(StoreErrorCode.BUSINESS_ALREADY_REGISTERED, error.errorCode());
        verify(storeMapper, never()).insert(any(Store.class));
    }

    @Test
    void 유효한_동일_사업자_검증은_외부호출과_insert없이_재사용한다() {
        LocalDateTime verifiedAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        BusinessVerification existing = BusinessVerification.builder()
                .id("existing-verification")
                .userId(20L)
                .businessRegistrationNumber("1234567890")
                .representativeName("대표자")
                .openingDate("20260101")
                .businessStatusCode("01")
                .businessStatusName("계속사업자")
                .verifiedAt(verifiedAt)
                .expiresAt(verifiedAt.plusMinutes(30))
                .build();

        when(storeMapper.findByUserId(20L)).thenReturn(null);
        when(storeMapper.existsByBusinessRegistrationNumber("1234567890"))
                .thenReturn(false);
        when(businessVerificationMapper.findReusableVerified(
                eq(20L),
                eq("1234567890"),
                eq("대표자"),
                eq("20260101"),
                any(LocalDateTime.class)
        )).thenReturn(existing);

        var response = verificationService.verify(
                20L,
                verificationRequest()
        );

        assertEquals(existing.getId(), response.verificationId());
        verify(ntsBusinessClient, never()).verify(anyString(), anyString(), anyString());
        verify(businessVerificationMapper, never()).insert(any(BusinessVerification.class));
    }

    @Test
    void 기존_검증이_만료되면_외부_재검증_후_새_행을_생성한다() {
        when(storeMapper.findByUserId(20L)).thenReturn(null);
        when(storeMapper.existsByBusinessRegistrationNumber("1234567890"))
                .thenReturn(false);
        when(businessVerificationMapper.findReusableVerified(
                eq(20L),
                eq("1234567890"),
                eq("대표자"),
                eq("20260101"),
                any(LocalDateTime.class)
        )).thenReturn(null);
        when(ntsBusinessClient.verify("1234567890", "대표자", "20260101"))
                .thenReturn(new NtsBusinessVerificationResult(
                        true,
                        "정상",
                        "01",
                        "계속사업자"
                ));
        when(businessVerificationMapper.insert(any(BusinessVerification.class)))
                .thenAnswer(invocation -> {
                    BusinessVerification inserted = invocation.getArgument(0);
                    assertNotEquals("expired-verification", inserted.getId());
                    return 1;
                });

        var response = verificationService.verify(
                20L,
                verificationRequest()
        );

        assertEquals("1234567890", response.businessRegistrationNumber());
        verify(ntsBusinessClient).verify("1234567890", "대표자", "20260101");
        verify(businessVerificationMapper).insert(any(BusinessVerification.class));
    }

    private BusinessVerificationRequest verificationRequest() {
        return new BusinessVerificationRequest(
                "123-45-67890",
                " 대표자 ",
                "2026-01-01"
        );
    }

    private StoreCreateRequest createRequest(String verificationId) {
        return new StoreCreateRequest(
                verificationId,
                "테스트 매장",
                "010-1234-5678",
                "대구광역시 중구 중앙대로 1",
                null,
                "음식점업",
                0,
                "09:00",
                "21:00"
        );
    }
}
