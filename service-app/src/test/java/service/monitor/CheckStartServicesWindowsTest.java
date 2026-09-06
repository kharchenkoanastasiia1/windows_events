package service.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.service.DateFormatter;
import org.windows_events.service.monitor.NetworkAddressMonitorService;
import org.windows_events.service.monitor.UserMonitorService;
import org.windows_events.service.monitor.services_windows.CheckStartServicesWindows;
import org.windows_events.time.NTPTimeService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CheckStartServicesWindowsTest {

    private DurableSeqLogger durableLogger;
    private Runtime runtime;
    private Process process;

    @BeforeEach
    void setUp() {
        durableLogger = mock(DurableSeqLogger.class);
        runtime = mock(Runtime.class);
        process = mock(Process.class);
    }

    // =====================================================================
    // isServiceRunning()
    // =====================================================================

    @Test
    void isServiceRunning_shouldReturnTrue_whenScQueryContainsRunning()
            throws Exception {

        String output =
                "SERVICE_NAME: TestService\n" +
                        "        TYPE               : 10  WIN32_OWN_PROCESS\n" +
                        "        STATE              : 4  RUNNING\n";

        when(process.getInputStream())
                .thenReturn(toInputStream(output));

        when(runtime.exec("sc query TestService"))
                .thenReturn(process);

        try (MockedStatic<Runtime> runtimeMock =
                     mockStatic(Runtime.class)) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            boolean result =
                    service.isServiceRunning(
                            "TestService"
                    );

            assertTrue(result);

            verify(runtime)
                    .exec("sc query TestService");
        }
    }

    @Test
    void isServiceRunning_shouldReturnFalse_whenServiceIsStopped()
            throws Exception {

        String output =
                "SERVICE_NAME: TestService\n" +
                        "        STATE              : 1  STOPPED\n";

        when(process.getInputStream())
                .thenReturn(toInputStream(output));

        when(runtime.exec("sc query TestService"))
                .thenReturn(process);

        try (MockedStatic<Runtime> runtimeMock =
                     mockStatic(Runtime.class)) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            boolean result =
                    service.isServiceRunning(
                            "TestService"
                    );

            assertFalse(result);
        }
    }

    @Test
    void isServiceRunning_shouldReturnFalse_whenCommandThrowsException()
            throws Exception {

        when(runtime.exec("sc query TestService"))
                .thenThrow(
                        new RuntimeException(
                                "sc unavailable"
                        )
                );

        try (MockedStatic<Runtime> runtimeMock =
                     mockStatic(Runtime.class)) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            boolean result =
                    service.isServiceRunning(
                            "TestService"
                    );

            assertFalse(result);
        }
    }

    // =====================================================================
    // isServiceAutoStart()
    // =====================================================================

    @Test
    void isServiceAutoStart_shouldReturnTrue_whenOutputContainsAutoStart()
            throws Exception {

        String output =
                "[SC] QueryServiceConfig SUCCESS\n" +
                        "START_TYPE         : 2   AUTO_START\n";

        when(process.getInputStream())
                .thenReturn(toInputStream(output));

        when(runtime.exec("sc qc TestService"))
                .thenReturn(process);

        try (MockedStatic<Runtime> runtimeMock =
                     mockStatic(Runtime.class)) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            boolean result =
                    service.isServiceAutoStart(
                            "TestService"
                    );

            assertTrue(result);

            verify(runtime)
                    .exec("sc qc TestService");
        }
    }

    @Test
    void isServiceAutoStart_shouldReturnFalse_whenServiceIsManual()
            throws Exception {

        String output =
                "START_TYPE         : 3   DEMAND_START\n";

        when(process.getInputStream())
                .thenReturn(toInputStream(output));

        when(runtime.exec("sc qc TestService"))
                .thenReturn(process);

        try (MockedStatic<Runtime> runtimeMock =
                     mockStatic(Runtime.class)) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            assertFalse(
                    service.isServiceAutoStart(
                            "TestService"
                    )
            );
        }
    }

    @Test
    void isServiceAutoStart_shouldReturnFalse_whenCommandFails()
            throws Exception {

        when(runtime.exec("sc qc TestService"))
                .thenThrow(
                        new RuntimeException(
                                "command failed"
                        )
                );

        try (MockedStatic<Runtime> runtimeMock =
                     mockStatic(Runtime.class)) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            assertFalse(
                    service.isServiceAutoStart(
                            "TestService"
                    )
            );
        }
    }

    // =====================================================================
    // startService()
    // =====================================================================

    @Test
    void startService_shouldLogEvent_whenCommandSucceedsAndNtpAvailable()
            throws Exception {

        Date ntpDate =
                Date.from(
                        Instant.parse(
                                "2026-09-06T12:00:00Z"
                        )
                );

        when(process.waitFor())
                .thenReturn(0);

        when(runtime.exec("sc start TestService"))
                .thenReturn(process);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedConstruction<NTPTimeService> ntpMock =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) -> {
                                    when(mock.getNTPTime())
                                            .thenReturn(ntpDate);
                                }
                        );

                MockedStatic<NetworkAddressMonitorService> pcMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> dateMock =
                        mockStatic(DateFormatter.class)
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            pcMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC-01");

            pcMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.50");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("test-user");

            dateMock.when(() ->
                            DateFormatter.dateConvert(
                                    ntpDate
                            )
                    )
                    .thenReturn(
                            " 06.09.2026 15:00:00"
                    );

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            service.startService(
                    "TestService"
            );

            verify(runtime)
                    .exec(
                            "sc start TestService"
                    );

            verify(process)
                    .waitFor();

            verify(durableLogger)
                    .log(
                            argThat(message ->
                                    message != null
                                            && message.contains("TestService")
                                            && message.contains("06.09.2026")
                                            && !message.contains("TIME_PC")
                            )
                    );
        }
    }

    @Test
    void startService_shouldUsePcTime_whenNtpReturnsNull()
            throws Exception {

        when(process.waitFor())
                .thenReturn(0);

        when(runtime.exec("sc start TestService"))
                .thenReturn(process);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedConstruction<NTPTimeService> ntpMock =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) -> {
                                    when(mock.getNTPTime())
                                            .thenReturn(null);
                                }
                        );

                MockedStatic<NetworkAddressMonitorService> pcMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> dateMock =
                        mockStatic(DateFormatter.class)
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            pcMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC-01");

            pcMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.50");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("test-user");

            /*
             * Дата создаётся внутри метода через Instant.now(),
             * поэтому точное значение заранее неизвестно.
             */
            dateMock.when(() ->
                            DateFormatter.dateConvert(
                                    any(Date.class)
                            )
                    )
                    .thenReturn(
                            " local-time"
                    );

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            service.startService(
                    "TestService"
            );

            verify(durableLogger)
                    .log(
                            argThat(message ->
                                    message != null
                                            && message.contains("TestService")
                                            && message.contains("local-time")
                            )
                    );
        }
    }

    @Test
    void startService_shouldNotLog_whenCommandFails()
            throws Exception {

        when(process.waitFor())
                .thenReturn(1);

        when(runtime.exec("sc start TestService"))
                .thenReturn(process);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedConstruction<NTPTimeService> ntpMock =
                        mockConstruction(
                                NTPTimeService.class
                        )
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            service.startService(
                    "TestService"
            );

            verify(durableLogger, never())
                    .log(anyString());

            /*
             * Если sc start вернул ненулевой код,
             * NTP вообще не нужен.
             */
            assertEquals(
                    1,
                    ntpMock.constructed().size()
            );

            verify(
                    ntpMock.constructed().get(0),
                    never()
            ).getNTPTime();
        }
    }

    // =====================================================================
    // setServiceAutoStart()
    // =====================================================================

    @Test
    void setServiceAutoStart_shouldLogEvent_whenCommandSucceeds()
            throws Exception {

        Date ntpDate =
                Date.from(
                        Instant.parse(
                                "2026-09-06T12:00:00Z"
                        )
                );

        when(process.waitFor())
                .thenReturn(0);

        when(runtime.exec(
                "sc config TestService start= auto"
        )).thenReturn(process);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedConstruction<NTPTimeService> ntpMock =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) -> {
                                    when(mock.getNTPTime())
                                            .thenReturn(ntpDate);
                                }
                        );

                MockedStatic<NetworkAddressMonitorService> pcMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> dateMock =
                        mockStatic(DateFormatter.class)
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            pcMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC-01");

            pcMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.50");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("test-user");

            dateMock.when(() ->
                            DateFormatter.dateConvert(
                                    ntpDate
                            )
                    )
                    .thenReturn(
                            " 06.09.2026 15:00:00"
                    );

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            service.setServiceAutoStart(
                    "TestService"
            );

            verify(runtime)
                    .exec(
                            "sc config TestService start= auto"
                    );

            verify(process)
                    .waitFor();

            verify(durableLogger)
                    .log(
                            argThat(message ->
                                    message != null
                                            && message.contains("TestService")
                                            && message.contains("06.09.2026")
                            )
                    );
        }
    }

    @Test
    void setServiceAutoStart_shouldNotLog_whenCommandFails()
            throws Exception {

        when(process.waitFor())
                .thenReturn(1);

        when(runtime.exec(
                "sc config TestService start= auto"
        )).thenReturn(process);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class)
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            CheckStartServicesWindows service =
                    new CheckStartServicesWindows(
                            durableLogger,
                            "pool.ntp.org"
                    );

            service.setServiceAutoStart(
                    "TestService"
            );

            verify(durableLogger, never())
                    .log(anyString());
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private InputStream toInputStream(
            String value
    ) {

        return new ByteArrayInputStream(
                value.getBytes()
        );
    }
}
