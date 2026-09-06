package service.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.service.monitor.NetworkConnectionSpeedMonitorService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NetworkConnectionSpeedMonitorServiceTest {

    private DurableSeqLogger logger;

    private NetworkConnectionSpeedMonitorService service;

    @BeforeEach
    void setUp() {

        logger =
                mock(DurableSeqLogger.class);

        service =
                new NetworkConnectionSpeedMonitorService(
                        logger
                );
    }

    // =========================================================
    // parseSpeedToMbps()
    // =========================================================

    @Test
    void parseSpeedToMbps_shouldParse100Mbps()
            throws Exception {

        Optional<Long> result =
                invokeParseSpeed("100 Mbps");

        assertTrue(result.isPresent());

        assertEquals(
                100L,
                result.get()
        );
    }

    @Test
    void parseSpeedToMbps_shouldParse1Gbps()
            throws Exception {

        Optional<Long> result =
                invokeParseSpeed("1 Gbps");

        assertEquals(
                Optional.of(1000L),
                result
        );
    }

    @Test
    void parseSpeedToMbps_shouldParseTwoPointFiveGbps()
            throws Exception {

        Optional<Long> result =
                invokeParseSpeed("2.5 Gbps");

        assertEquals(
                Optional.of(2500L),
                result
        );
    }

    @Test
    void parseSpeedToMbps_shouldParseCommaDecimal()
            throws Exception {

        Optional<Long> result =
                invokeParseSpeed("2,5 Gbps");

        assertEquals(
                Optional.of(2500L),
                result
        );
    }

    @Test
    void parseSpeedToMbps_shouldParseBitsPerSecond()
            throws Exception {

        Optional<Long> result =
                invokeParseSpeed(
                        "100000000 bps"
                );

        assertEquals(
                Optional.of(100L),
                result
        );
    }

    @Test
    void parseSpeedToMbps_shouldConvertZeroBpsToZeroMbps()
            throws Exception {

        Optional<Long> result =
                invokeParseSpeed("0 bps");

        assertTrue(result.isPresent());

        assertEquals(
                0L,
                result.get()
        );
    }

    @Test
    void parseSpeedToMbps_shouldReturnEmpty_whenValueIsInvalid()
            throws Exception {

        assertTrue(
                invokeParseSpeed("Unknown")
                        .isEmpty()
        );
    }

    @Test
    void parseSpeedToMbps_shouldReturnEmpty_whenValueIsBlank()
            throws Exception {

        assertTrue(
                invokeParseSpeed("")
                        .isEmpty()
        );
    }

    @Test
    void parseSpeedToMbps_shouldReturnEmpty_whenValueIsNull()
            throws Exception {

        assertTrue(
                invokeParseSpeed(null)
                        .isEmpty()
        );
    }

    // =========================================================
    // parseAdapters()
    // =========================================================

    @Test
    void parseAdapters_shouldParseArray()
            throws Exception {

        String json =
                "[" +
                        "{\"Name\":\"Ethernet\",\"LinkSpeed\":\"100 Mbps\"}," +
                        "{\"Name\":\"Wi-Fi\",\"LinkSpeed\":\"1 Gbps\"}" +
                        "]";

        List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
                result = invokeParseAdapters(json);

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                "Ethernet",
                result.get(0).getName()
        );

        assertEquals(
                "100 Mbps",
                result.get(0).getRawLinkSpeed()
        );

        assertEquals(
                Optional.of(100L),
                result.get(0).getSpeedMbps()
        );

        assertEquals(
                "Wi-Fi",
                result.get(1).getName()
        );

        assertEquals(
                Optional.of(1000L),
                result.get(1).getSpeedMbps()
        );
    }

    @Test
    void parseAdapters_shouldParseSingleObject()
            throws Exception {

        String json =
                """
                {
                  "Name":"Ethernet",
                  "LinkSpeed":"100 Mbps"
                }
                """;

        List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
                result = invokeParseAdapters(json);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "Ethernet",
                result.get(0).getName()
        );

        assertEquals(
                Optional.of(100L),
                result.get(0).getSpeedMbps()
        );
    }

    @Test
    void parseAdapters_shouldReturnEmptyList_whenJsonIsBlank()
            throws Exception {

        List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
                result = invokeParseAdapters("");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseAdapters_shouldReturnEmptyList_whenJsonIsNull()
            throws Exception {

        List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
                result = invokeParseAdapters(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseAdapters_shouldHandleZeroBps()
            throws Exception {

        String json =
                """
                {
                  "Name":"Ethernet",
                  "LinkSpeed":"0 bps"
                }
                """;

        List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
                result = invokeParseAdapters(json);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                Optional.of(0L),
                result.get(0).getSpeedMbps()
        );
    }

    @Test
    void parseAdapters_shouldLeaveInvalidSpeedEmpty()
            throws Exception {

        String json =
                """
                {
                  "Name":"Ethernet",
                  "LinkSpeed":"Unknown"
                }
                """;

        List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
                result = invokeParseAdapters(json);

        assertEquals(
                1,
                result.size()
        );

        assertTrue(
                result.get(0)
                        .getSpeedMbps()
                        .isEmpty()
        );
    }

    // =========================================================
    // Helpers
    // =========================================================

    @SuppressWarnings("unchecked")
    private Optional<Long> invokeParseSpeed(
            String value
    ) throws Exception {

        Method method =
                NetworkConnectionSpeedMonitorService.class
                        .getDeclaredMethod(
                                "parseSpeedToMbps",
                                String.class
                        );

        method.setAccessible(true);

        return (Optional<Long>)
                method.invoke(
                        service,
                        value
                );
    }

    @SuppressWarnings("unchecked")
    private List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>
    invokeParseAdapters(String json)
            throws Exception {

        Method method =
                NetworkConnectionSpeedMonitorService.class
                        .getDeclaredMethod(
                                "parseAdapters",
                                String.class
                        );

        method.setAccessible(true);

        return (List<NetworkConnectionSpeedMonitorService.AdapterSpeedInfo>)
                method.invoke(
                        service,
                        json
                );
    }
}
