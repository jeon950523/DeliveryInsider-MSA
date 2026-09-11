package com.deliveryinsider.store.domain.store.service;

import com.deliveryinsider.store.domain.store.entity.BusinessVerification;
import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.enums.OperationStatus;
import com.deliveryinsider.store.domain.store.mapper.BusinessVerificationMapper;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.domain.store.request.StoreCreateRequest;
import com.deliveryinsider.store.domain.store.request.StoreUpdateRequest;
import com.deliveryinsider.store.domain.store.response.StoreResponse;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StoreService {

    private static final String CONTINUING_BUSINESS_CODE = "01";

    private final StoreMapper storeMapper;
    private final com.deliveryinsider.store.domain.catalog.CatalogEventWriter catalogEvents;
    private final BusinessVerificationMapper businessVerificationMapper;

    @Transactional(readOnly = true)
    public StoreResponse findMyStore(Long userId) {
        Store store = Optional.ofNullable(
                        storeMapper.findByUserId(userId)
                )
                .orElseThrow(() ->
                        new BusinessException(
                                StoreErrorCode.STORE_NOT_FOUND
                        )
                );

        return toStoreResponse(store);
    }

    @Transactional
    public StoreResponse create(
            Long userId,
            StoreCreateRequest request
    ) {
        if (storeMapper.findByUserId(userId) != null) {
            throw new BusinessException(
                    StoreErrorCode.STORE_ALREADY_EXISTS
            );
        }

        BusinessVerification verification = Optional.ofNullable(
                        businessVerificationMapper.findByIdForUpdate(
                                request.businessVerificationId()
                        )
                )
                .orElseThrow(() ->
                        new BusinessException(
                                StoreErrorCode.BUSINESS_VERIFICATION_NOT_FOUND
                        )
                );

        if (!userId.equals(
                verification.getUserId()
        )) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_NOT_FOUND
            );
        }

        if (verification.getConsumedAt() != null) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_ALREADY_USED
            );
        }

        LocalDateTime now = LocalDateTime.now(
                ZoneOffset.UTC
        );

        if (verification.getExpiresAt() == null
                || !verification.getExpiresAt().isAfter(now)) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_EXPIRED
            );
        }

        if (!CONTINUING_BUSINESS_CODE.equals(
                verification.getBusinessStatusCode()
        )) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_FAILED
            );
        }

        if (storeMapper.existsByBusinessRegistrationNumber(
                verification.getBusinessRegistrationNumber()
        )) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_ALREADY_REGISTERED
            );
        }

        Store store = Store.builder()
                .userId(userId)
                .storeName(request.storeName().trim())
                .phone(blankToNull(request.phone()))
                .businessRegistrationNumber(
                        verification.getBusinessRegistrationNumber()
                )
                .businessVerificationId(
                        verification.getId()
                )
                .address(request.address().trim())
                .addressDetail(blankToNull(request.addressDetail()))
                .industryType(request.industryType().trim())
                .minimumOrderAmount(
                        Optional.ofNullable(request.minimumOrderAmount())
                                .orElse(0)
                )
                .openTime(request.openTime())
                .closeTime(request.closeTime())
                .operationStatus(OperationStatus.OPERATING)
                .build();

        int inserted = storeMapper.insert(store);

        if (inserted != 1 || store.getId() == null) {
            throw new BusinessException(
                    StoreErrorCode.STORE_CREATE_FAILED
            );
        }

        int consumed = businessVerificationMapper.markConsumed(
                verification.getId(),
                userId,
                now
        );

        if (consumed != 1) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_ALREADY_USED
            );
        }

        Store createdStore = Optional.ofNullable(
                        storeMapper.findById(store.getId())
                )
                .orElseThrow(() ->
                        new BusinessException(
                                StoreErrorCode.STORE_CREATE_FAILED
                        )
                );

        catalogEvents.storeChanged(createdStore.getId(), "STORE_CREATED");
        return toStoreResponse(createdStore);
    }

    @Transactional(rollbackFor = Exception.class)
    public StoreResponse update(
            Long userId,
            StoreUpdateRequest request
    ) {
        Store currentStore = Optional.ofNullable(
                        storeMapper.findByUserId(userId)
                )
                .orElseThrow(() ->
                        new BusinessException(
                                StoreErrorCode.STORE_NOT_FOUND
                        )
                );

        Store updateStore = Store.builder()
                .id(currentStore.getId())
                .userId(userId)
                .storeName(request.storeName())
                .phone(request.phone())
                .address(request.address())
                .addressDetail(request.addressDetail())
                .industryType(request.industryType())
                .minimumOrderAmount(request.minimumOrderAmount())
                .openTime(request.openTime())
                .closeTime(request.closeTime())
                .operationStatus(request.operationStatus())
                .build();

        int result = storeMapper.update(updateStore);

        if (result != 1) {
            throw new BusinessException(
                    StoreErrorCode.IN_EDIT_STORE_ERROR
            );
        }

        Store updatedStore = storeMapper.findByUserId(userId);

        catalogEvents.storeChanged(updatedStore.getId(), "STORE_UPDATED");
        return toStoreResponse(updatedStore);
    }

    private StoreResponse toStoreResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .userId(store.getUserId())
                .storeName(store.getStoreName())
                .phone(store.getPhone())
                .businessRegistrationNumber(
                        store.getBusinessRegistrationNumber()
                )
                .businessVerificationId(
                        store.getBusinessVerificationId()
                )
                .address(store.getAddress())
                .addressDetail(store.getAddressDetail())
                .industryType(store.getIndustryType())
                .minimumOrderAmount(store.getMinimumOrderAmount())
                .openTime(store.getOpenTime())
                .closeTime(store.getCloseTime())
                .operationStatus(store.getOperationStatus())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    private String blankToNull(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
