package com.deliveryinsider.order.integration.store.dto;

import java.util.List;

public record StoreOrderSnapshotRequest(

    List<Long> menuIds

) {
}
