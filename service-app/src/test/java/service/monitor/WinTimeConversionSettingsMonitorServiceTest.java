package service.monitor;

import org.junit.jupiter.api.Test;
import org.windows_events.service.monitor.WinTimeConversionSettingsMonitorService;

import static org.junit.jupiter.api.Assertions.*;

class WinTimeConversionSettingsMonitorServiceTest {

    @Test
    void dynamicTimeZoneInfo_shouldInitiallyHaveDstEnabled() {

        WinTimeConversionSettingsMonitorService
                .DYNAMIC_TIME_ZONE_INFORMATION info =
                new WinTimeConversionSettingsMonitorService
                        .DYNAMIC_TIME_ZONE_INFORMATION();

        info.DynamicDaylightTimeDisabled = 0;

        assertFalse(
                info.isDynamicDstDisabled()
        );
    }

    @Test
    void setDynamicDstDisabled_shouldDisableDst() {

        WinTimeConversionSettingsMonitorService
                .DYNAMIC_TIME_ZONE_INFORMATION info =
                new WinTimeConversionSettingsMonitorService
                        .DYNAMIC_TIME_ZONE_INFORMATION();

        info.setDynamicDstDisabled(true);

        assertTrue(
                info.isDynamicDstDisabled()
        );

        assertEquals(
                1,
                info.DynamicDaylightTimeDisabled
        );
    }

    @Test
    void setDynamicDstDisabledFalse_shouldEnableDst() {

        WinTimeConversionSettingsMonitorService
                .DYNAMIC_TIME_ZONE_INFORMATION info =
                new WinTimeConversionSettingsMonitorService
                        .DYNAMIC_TIME_ZONE_INFORMATION();

        info.setDynamicDstDisabled(true);

        info.setDynamicDstDisabled(false);

        assertFalse(
                info.isDynamicDstDisabled()
        );

        assertEquals(
                0,
                info.DynamicDaylightTimeDisabled
        );
    }

    @Test
    void getTimeZoneKeyName_shouldReturnStringUntilNullCharacter() {

        WinTimeConversionSettingsMonitorService
                .DYNAMIC_TIME_ZONE_INFORMATION info =
                new WinTimeConversionSettingsMonitorService
                        .DYNAMIC_TIME_ZONE_INFORMATION();

        String value =
                "FLE Standard Time";

        for (
                int i = 0;
                i < value.length();
                i++
        ) {

            info.TimeZoneKeyName[i] =
                    value.charAt(i);
        }

        info.TimeZoneKeyName[
                value.length()
                ] = '\0';

        assertEquals(
                "FLE Standard Time",
                info.getTimeZoneKeyName()
        );
    }

    @Test
    void getTimeZoneKeyName_shouldReturnEmptyString_whenArrayIsEmpty() {

        WinTimeConversionSettingsMonitorService
                .DYNAMIC_TIME_ZONE_INFORMATION info =
                new WinTimeConversionSettingsMonitorService
                        .DYNAMIC_TIME_ZONE_INFORMATION();

        assertEquals(
                "",
                info.getTimeZoneKeyName()
        );
    }
}
