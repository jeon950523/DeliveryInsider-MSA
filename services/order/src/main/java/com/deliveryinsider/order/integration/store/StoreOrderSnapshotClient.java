package com.deliveryinsider.order.integration.store;

import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotRequest;
import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotResponse;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import com.deliveryinsider.order.messaging.platform.exception.RetryableOrderEventProcessingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class StoreOrderSnapshotClient {

    private final RestClient restClient;

    public StoreOrderSnapshotClient(
        @Qualifier("storeRestClient")
        RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public StoreOrderSnapshotResponse fetch(
        Long storeId,
        List<Long> menuIds
    ) {
        List<Long> requestedMenuIds =
            normalizeMenuIds(menuIds);

        StoreOrderSnapshotResponse response;

        try {
            response = restClient
                .post()
                .uri(
                    "/internal/stores/{storeId}/order-snapshots",
                    storeId
                )
                .body(
                    new StoreOrderSnapshotRequest(
                        requestedMenuIds
                    )
                )
                .retrieve()
                .body(
                    StoreOrderSnapshotResponse.class
                );

        } catch (ResourceAccessException e) {
            throw new RetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_CONNECTION_FAILED",
                "Store Snapshot API 연결에 실패했습니다.",
                e
            );

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                throw new RetryableOrderEventProcessingException(
                    "STORE_SNAPSHOT_UNAVAILABLE",
                    "Store Snapshot API에 일시적인 장애가 발생했습니다.",
                    e
                );
            }

            throw new NonRetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_REJECTED",
                "Store가 주문 Snapshot 요청을 거부했습니다.",
                e
            );
        }

        validateResponse(
            storeId,
            requestedMenuIds,
            response
        );

        return response;
    }

    private List<Long> normalizeMenuIds(
        List<Long> menuIds
    ) {
        if (menuIds == null) {
            throw new NonRetryableOrderEventProcessingException(
                "ORDER_MENU_IDS_EMPTY",
                "주문 Menu ID가 없습니다."
            );
        }

        List<Long> normalized =
            menuIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            throw new NonRetryableOrderEventProcessingException(
                "ORDER_MENU_IDS_EMPTY",
                "주문 Menu ID가 없습니다."
            );
        }

        return normalized;
    }

    private void validateResponse(
        Long requestedStoreId,
        List<Long> requestedMenuIds,
        StoreOrderSnapshotResponse response
    ) {
        if (response == null) {
            throw new NonRetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_EMPTY_RESPONSE",
                "Store Snapshot 응답이 없습니다."
            );
        }

        if (!Objects.equals(
            requestedStoreId,
            response.storeId()
        )) {
            throw new NonRetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_STORE_MISMATCH",
                "요청 Store와 Snapshot Store가 일치하지 않습니다."
            );
        }

        List<Long> responseMenuIds =
            response.menus()
                .stream()
                .map(
                    StoreOrderSnapshotResponse
                        .MenuSnapshot::menuId
                )
                .toList();

        if (responseMenuIds.stream().anyMatch(Objects::isNull)) {
            throw new NonRetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_MENU_MISMATCH",
                "Snapshot 응답에 유효하지 않은 Menu ID가 있습니다."
            );
        }

        Set<Long> requestedSet =
            Set.copyOf(requestedMenuIds);

        Set<Long> responseSet =
            new HashSet<>(responseMenuIds);

        boolean duplicateResponseMenu =
            responseSet.size()
                != responseMenuIds.size();

        if (
            duplicateResponseMenu
                || !requestedSet.equals(responseSet)
        ) {
            throw new NonRetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_MENU_MISMATCH",
                "요청 Menu와 Snapshot 응답 Menu가 일치하지 않습니다."
            );
        }
    }
}
