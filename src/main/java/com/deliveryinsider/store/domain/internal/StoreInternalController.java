package com.deliveryinsider.store.domain.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/stores")
public class StoreInternalController {

    private final StoreInternalService service;

    public StoreInternalController(StoreInternalService service) {
        this.service = service;
    }

    @GetMapping("/users/{userId}")
    public StoreInternalService.OwnedStore findOwnedStore(@PathVariable @Positive long userId) {
        return service.findOwnedStore(userId);
    }

    @PostMapping("/{storeId}/menus")
    public StoreInternalService.CreatedMenu createMenu(
        @PathVariable @Positive long storeId,
        @Valid @RequestBody CreateMenuRequest request
    ) {
        return service.createMenu(
            storeId,
            new StoreInternalService.CreateMenuCommand(
                request.operationKey(),
                request.menuName(),
                request.menuPrice(),
                request.menuCost(),
                request.packagingFee(),
                request.expectedCookingTime()
            )
        );
    }

    @PostMapping("/{storeId}/order-snapshots")
    public StoreInternalService.OrderSnapshot orderSnapshot(
        @PathVariable @Positive long storeId,
        @Valid @RequestBody OrderSnapshotRequest request
    ) {
        return service.orderSnapshot(storeId, request.menuIds());
    }

    public record CreateMenuRequest(
        @NotBlank @Size(max = 180) String operationKey,
        @NotBlank @Size(max = 100) String menuName,
        @NotNull @Min(0) Integer menuPrice,
        @NotNull @Min(0) Integer menuCost,
        @NotNull @Min(0) Integer packagingFee,
        @NotNull @Min(1) @Max(1440) Integer expectedCookingTime
    ) {}

    public record OrderSnapshotRequest(
        @NotEmpty List<@NotNull @Positive Long> menuIds
    ) {}
}
