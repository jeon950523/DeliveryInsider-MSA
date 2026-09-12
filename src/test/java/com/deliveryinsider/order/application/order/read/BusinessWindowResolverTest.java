package com.deliveryinsider.order.application.order.read;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessWindowResolverTest {

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
}
