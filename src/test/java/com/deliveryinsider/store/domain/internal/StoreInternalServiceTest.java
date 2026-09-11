package com.deliveryinsider.store.domain.internal;

import com.deliveryinsider.store.global.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreInternalServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StoreInternalService service;

    @Test
    void findsTheActiveStoreOwnedByTheUser() {
        when(jdbcTemplate.query(contains("FROM stores"), any(RowMapper.class), eq(8L)))
            .thenReturn(List.of(new StoreInternalService.OwnedStore(3L, "test")));

        StoreInternalService.OwnedStore result = service.findOwnedStore(8L);

        assertEquals(3L, result.storeId());
        assertEquals("test", result.storeName());
    }

    @Test
    void rejectsAUserWithoutAnActiveStore() {
        when(jdbcTemplate.query(contains("FROM stores"), any(RowMapper.class), eq(999L)))
            .thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.findOwnedStore(999L));
    }
}
