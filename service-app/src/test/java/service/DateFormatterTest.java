package service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.windows_events.service.DateFormatter;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateFormatterTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {

        originalTimeZone =
                TimeZone.getDefault();

        /*
         * Фиксируем timezone,
         * чтобы тест не зависел от ПК/CI.
         */
        TimeZone.setDefault(
                TimeZone.getTimeZone("UTC")
        );
    }

    @AfterEach
    void tearDown() {

        TimeZone.setDefault(
                originalTimeZone
        );
    }

    @Test
    void dateConvertString_shouldFormatTimestamp() {

        /*
         * 2026-09-06T12:00:00Z
         */
        long timestamp =
                1788696000000L;

        String result =
                DateFormatter.dateConvertString(
                        timestamp
                );

        assertEquals(
                "06.09.2026 12:00:00",
                result
        );
    }

    @Test
    void dateConvert_shouldFormatDate() {

        long timestamp =
                1788696000000L;

        Date date =
                new Date(timestamp);

        String result =
                DateFormatter.dateConvert(date);

        assertEquals(
                "06.09.2026 12:00:00",
                result
        );
    }

    @Test
    void dateConvertAndDateConvertString_shouldReturnSameResult() {

        long timestamp =
                1788696000000L;

        String fromLong =
                DateFormatter.dateConvertString(
                        timestamp
                );

        String fromDate =
                DateFormatter.dateConvert(
                        new Date(timestamp)
                );

        assertEquals(
                fromLong,
                fromDate
        );
    }

    @Test
    void dateConvertString_shouldFormatEpoch() {

        String result =
                DateFormatter.dateConvertString(0L);

        assertEquals(
                "01.01.1970 00:00:00",
                result
        );
    }
}
