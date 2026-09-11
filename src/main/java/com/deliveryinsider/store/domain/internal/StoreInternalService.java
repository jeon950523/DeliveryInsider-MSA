package com.deliveryinsider.store.domain.internal;

import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class StoreInternalService {

    private final JdbcTemplate jdbcTemplate;

    public StoreInternalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public OwnedStore findOwnedStore(long userId) {
        List<OwnedStore> stores = jdbcTemplate.query(
            "SELECT id, store_name FROM stores WHERE user_id = ? AND deleted_at IS NULL ORDER BY id LIMIT 1",
            (rs, rowNum) -> new OwnedStore(rs.getLong("id"), rs.getString("store_name")),
            userId
        );
        if (stores.isEmpty()) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }
        return stores.getFirst();
    }

    @Transactional
    public CreatedMenu createMenu(long storeId, CreateMenuCommand command) {
        CreatedMenu completed = findCompletedOperation(command.operationKey(), storeId);
        if (completed != null) {
            return completed;
        }

        Integer storeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM stores WHERE id = ? AND deleted_at IS NULL",
            Integer.class,
            storeId
        );
        if (storeCount == null || storeCount == 0) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }

        try {
            jdbcTemplate.update(
                "INSERT INTO internal_menu_creation_operations (operation_key, store_id) VALUES (?, ?)",
                command.operationKey(),
                storeId
            );
        } catch (DuplicateKeyException duplicate) {
            CreatedMenu concurrentResult = findCompletedOperation(command.operationKey(), storeId);
            if (concurrentResult != null) {
                return concurrentResult;
            }
            throw duplicate;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO menus (store_id, menu_name, menu_price, menu_cost, packaging_fee, expected_cooking_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, storeId);
            statement.setString(2, command.menuName());
            statement.setInt(3, command.menuPrice());
            statement.setInt(4, command.menuCost());
            statement.setInt(5, command.packagingFee());
            statement.setInt(6, command.expectedCookingTime());
            return statement;
        }, keyHolder);
        Number menuId = keyHolder.getKey();
        if (inserted != 1 || menuId == null) {
            throw new IllegalStateException("Internal menu creation did not return an id");
        }

        jdbcTemplate.update(
            "UPDATE internal_menu_creation_operations SET menu_id = ?, completed_at = CURRENT_TIMESTAMP(6) "
                + "WHERE operation_key = ? AND store_id = ?",
            menuId.longValue(),
            command.operationKey(),
            storeId
        );
        return new CreatedMenu(menuId.longValue(), command.menuName(), command.menuPrice());
    }

    @Transactional(readOnly = true)
    public OrderSnapshot orderSnapshot(long storeId, List<Long> menuIds) {
        Integer storeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM stores WHERE id = ? AND deleted_at IS NULL",
            Integer.class,
            storeId
        );
        if (storeCount == null || storeCount == 0) {
            throw new BusinessException(StoreErrorCode.STORE_NOT_FOUND);
        }

        List<Long> requestedMenuIds = menuIds.stream().distinct().toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(requestedMenuIds.size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(storeId);
        parameters.addAll(requestedMenuIds);
        List<MenuSnapshot> menus = jdbcTemplate.query(
            "SELECT id, menu_name, menu_price, menu_cost, packaging_fee, expected_cooking_time "
                + "FROM menus WHERE store_id = ? AND deleted_at IS NULL AND menu_status = 'ACTIVE' "
                + "AND id IN (" + placeholders + ") ORDER BY id",
            (rs, rowNum) -> new MenuSnapshot(
                rs.getLong("id"),
                rs.getString("menu_name"),
                rs.getLong("menu_price"),
                rs.getLong("menu_cost"),
                rs.getLong("packaging_fee"),
                rs.getInt("expected_cooking_time")
            ),
            parameters.toArray()
        );
        if (menus.size() != requestedMenuIds.size()) {
            throw new BusinessException(StoreErrorCode.MENU_NOT_FOUND);
        }
        return new OrderSnapshot(storeId, menus);
    }

    private CreatedMenu findCompletedOperation(String operationKey, long storeId) {
        List<CreatedMenu> results = jdbcTemplate.query(
            "SELECT menu.id, menu.menu_name, menu.menu_price "
                + "FROM internal_menu_creation_operations operation "
                + "JOIN menus menu ON menu.id = operation.menu_id "
                + "WHERE operation.operation_key = ? AND operation.store_id = ? AND operation.completed_at IS NOT NULL "
                + "AND menu.deleted_at IS NULL",
            (rs, rowNum) -> new CreatedMenu(
                rs.getLong("id"),
                rs.getString("menu_name"),
                rs.getInt("menu_price")
            ),
            operationKey,
            storeId
        );
        return results.isEmpty() ? null : results.getFirst();
    }

    public record OwnedStore(Long storeId, String storeName) {}

    public record CreateMenuCommand(
        String operationKey,
        String menuName,
        int menuPrice,
        int menuCost,
        int packagingFee,
        int expectedCookingTime
    ) {}

    public record CreatedMenu(Long id, String menuName, Integer menuPrice) {}

    public record OrderSnapshot(Long storeId, List<MenuSnapshot> menus) {}

    public record MenuSnapshot(
        Long menuId,
        String menuName,
        long menuPrice,
        long menuCost,
        long packagingCost,
        Integer expectedCookingTime
    ) {}
}
