package com.deliveryinsider.order.application.order.read;

import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessWindowResolverTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    void resolvesSameDayBusinessHours() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 12, 30),
            "09:00",
            "18:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 12, 9, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 18, 0),
            window.businessEndAt()
        );
    }

    @Test
    void preservesOvernightBusinessHoursAfterMidnight() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 1, 30),
            "18:00",
            "03:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 11, 18, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 3, 0),
            window.businessEndAt()
        );
    }

    @Test
    void treatsEqualTimesAsTwentyFourHourBusinessWindow() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 8, 59),
            "09:00",
            "09:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 11, 9, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 9, 0),
            window.businessEndAt()
        );
    }

    @Test
    void treatsEqualSecondPrecisionTimesAsTwentyFourHourBusinessWindow() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 8, 59),
            "09:00:00",
            "09:00:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 11, 9, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 9, 0),
            window.businessEndAt()
        );
    }

    @Test
    void resolvesSameDayBusinessHoursWithSecondPrecisionTimes() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 12, 30),
            "09:00:00",
            "18:00:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 12, 9, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 18, 0),
            window.businessEndAt()
        );
    }

    @Test
    void preservesOvernightBusinessHoursWithSecondPrecisionTimes() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 1, 30),
            "18:00:00",
            "03:00:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 11, 18, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 3, 0),
            window.businessEndAt()
        );
    }

    @Test
    void acceptsSingleDigitHourTimes() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            LocalDateTime.of(2026, 9, 12, 12, 30),
            "9:00",
            "18:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 12, 9, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 18, 0),
            window.businessEndAt()
        );
    }

    @Test
    void convertsTwentyFourHourSeoulWindowToUtcDatabaseRange() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            Instant.parse("2026-09-12T08:59:36Z"),
            SEOUL,
            "09:00",
            "09:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 12, 0, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 13, 0, 0),
            window.businessEndAt()
        );
    }

    @Test
    void convertsSameDaySeoulWindowToUtcDatabaseRange() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            Instant.parse("2026-09-12T03:30:00Z"),
            SEOUL,
            "09:00",
            "18:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 12, 0, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 12, 9, 0),
            window.businessEndAt()
        );
    }

    @Test
    void convertsOvernightSeoulWindowAfterMidnightToUtcDatabaseRange() {
        BusinessWindow window = BusinessWindowResolver.resolve(
            Instant.parse("2026-09-11T16:30:00Z"),
            SEOUL,
            "18:00",
            "03:00"
        );

        assertEquals(
            LocalDateTime.of(2026, 9, 11, 9, 0),
            window.businessStartAt()
        );
        assertEquals(
            LocalDateTime.of(2026, 9, 11, 18, 0),
            window.businessEndAt()
        );
    }

    @Test
    void rejectsMissingBlankAndInvalidBusinessHours() {
        assertInvalidBusinessHours(null, "09:00");
        assertInvalidBusinessHours(" ", "09:00");
        assertInvalidBusinessHours("09:00", "invalid");
    }

    private static void assertInvalidBusinessHours(
        String openTime,
        String closeTime
    ) {
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> BusinessWindowResolver.resolve(
                LocalDateTime.of(2026, 9, 12, 12, 30),
                openTime,
                closeTime
            )
        );

        assertEquals(
            OrderErrorCode.STORE_BUSINESS_HOURS_INVALID,
            exception.errorCode()
        );
    }
}
