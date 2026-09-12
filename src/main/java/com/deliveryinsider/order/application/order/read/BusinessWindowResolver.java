package com.deliveryinsider.order.application.order.read;

import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * Resolves the most recently opened Store business window.  This preserves
 * overnight orders after midnight and treats equal opening/closing times as
 * a full 24-hour window rather than an empty range.
 */
public final class BusinessWindowResolver {

    private static final DateTimeFormatter TIME_FORMAT =
        new DateTimeFormatterBuilder()
            .appendPattern("H:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter();

    private BusinessWindowResolver() {
    }

    public static BusinessWindow resolve(
        LocalDateTime now,
        String openTime,
        String closeTime
    ) {
        LocalTime open = parse(openTime);
        LocalTime close = parse(closeTime);
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        if (open.equals(close)) {
            LocalDate startDate = currentTime.isBefore(open)
                ? today.minusDays(1)
                : today;

            return window(startDate, open, startDate.plusDays(1), close);
        }

        if (open.isBefore(close)) {
            LocalDate startDate = currentTime.isBefore(open)
                ? today.minusDays(1)
                : today;

            return window(startDate, open, startDate, close);
        }

        if (!currentTime.isBefore(open)) {
            return window(today, open, today.plusDays(1), close);
        }

        return window(today.minusDays(1), open, today, close);
    }

    public static BusinessWindow resolve(
        Instant now,
        ZoneId businessZone,
        String openTime,
        String closeTime
    ) {
        LocalDateTime localNow = LocalDateTime.ofInstant(
            now,
            businessZone
        );
        BusinessWindow localWindow = resolve(
            localNow,
            openTime,
            closeTime
        );

        return new BusinessWindow(
            toUtc(localWindow.businessStartAt(), businessZone),
            toUtc(localWindow.businessEndAt(), businessZone)
        );
    }

    private static LocalDateTime toUtc(
        LocalDateTime localDateTime,
        ZoneId businessZone
    ) {
        return localDateTime
            .atZone(businessZone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime();
    }

    private static BusinessWindow window(
        LocalDate startDate,
        LocalTime open,
        LocalDate endDate,
        LocalTime close
    ) {
        return new BusinessWindow(
            startDate.atTime(open),
            endDate.atTime(close)
        );
    }

    private static LocalTime parse(String value) {
        if (value == null || value.isBlank()) {
            throw invalidBusinessHours();
        }

        try {
            return LocalTime.parse(value.trim(), TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw invalidBusinessHours();
        }
    }

    private static BusinessException invalidBusinessHours() {
        return new BusinessException(
            OrderErrorCode.STORE_BUSINESS_HOURS_INVALID
        );
    }
}
