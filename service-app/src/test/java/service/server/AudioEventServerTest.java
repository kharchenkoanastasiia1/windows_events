package service.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.windows_events.audio.AudioEvent;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.service.DateFormatter;
import org.windows_events.service.monitor.NetworkAddressMonitorService;
import org.windows_events.service.monitor.UserMonitorService;
import org.windows_events.service.server.AudioEventServer;
import org.windows_events.time.NTPTimeService;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AudioEventServerTest {

    private DurableSeqLogger logger;
    private AudioEventServer server;
    private AudioEvent event;

    @BeforeEach
    void setUp() {
        logger = mock(DurableSeqLogger.class);
        event = mock(AudioEvent.class);

        server = new AudioEventServer(
                logger,
                47632,
                "pool.ntp.org"
        );
    }

    // ============================================================
    // buildSeqMessage()
    // ============================================================

    @Test
    void buildSeqMessage_shouldBuildMessageWithAudioEvent()
            throws Exception {

        Date ntpDate = Date.from(
                Instant.parse("2026-09-06T12:00:00Z")
        );

        when(event.getProcessName())
                .thenReturn("chrome.exe");

        when(event.getPid())
                .thenReturn(1234);

        when(event.getAction())
                .thenReturn("MUTE");

        when(event.getCurrentVolume())
                .thenReturn(0.25f);

        when(event.isVolumeRestoredToMax())
                .thenReturn(false);

        try (
                MockedConstruction<NTPTimeService> ntpMock =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(ntpDate)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC-01");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.100");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("test-user");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(ntpDate)
                    )
                    .thenReturn("06.09.2026 15:00:00");

            String result =
                    invokeBuildSeqMessage(event);

            assertNotNull(result);

            assertTrue(
                    result.contains("chrome.exe")
            );

            assertTrue(
                    result.contains("1234")
            );

            assertTrue(
                    result.contains("MUTE")
            );

            /*
             * 0.25 * 100 = 25%
             */
            assertTrue(
                    result.contains("25 %")
            );

            assertTrue(
                    result.contains(
                            "Заборонено вимкнення звуку"
                    )
            );

            assertFalse(
                    result.contains(
                            "гучність відновлена"
                    )
            );

            assertEquals(
                    1,
                    ntpMock.constructed().size()
            );

            verify(
                    ntpMock.constructed().get(0)
            ).getNTPTime();
        }
    }

    @Test
    void buildSeqMessage_shouldAddRestoredText_whenVolumeWasRestored()
            throws Exception {

        Date ntpDate = new Date(1000);

        when(event.getProcessName())
                .thenReturn("vlc.exe");

        when(event.getPid())
                .thenReturn(500);

        when(event.getAction())
                .thenReturn("VOLUME_ZERO");

        when(event.getCurrentVolume())
                .thenReturn(0.0f);

        when(event.isVolumeRestoredToMax())
                .thenReturn(true);

        try (
                MockedConstruction<NTPTimeService> ignored =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(ntpDate)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.1");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(ntpDate)
                    )
                    .thenReturn("DATE");

            String result =
                    invokeBuildSeqMessage(event);

            assertTrue(
                    result.contains(
                            "гучність відновлена"
                    )
            );

            assertTrue(
                    result.contains("0 %")
            );
        }
    }

    @Test
    void buildSeqMessage_shouldHandleNullProcessNameAndAction()
            throws Exception {

        Date ntpDate = new Date(1000);

        when(event.getProcessName())
                .thenReturn(null);

        when(event.getAction())
                .thenReturn(null);

        when(event.getPid())
                .thenReturn(10);

        when(event.getCurrentVolume())
                .thenReturn(0.5f);

        try (
                MockedConstruction<NTPTimeService> ignored =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(ntpDate)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("10.0.0.1");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(ntpDate)
                    )
                    .thenReturn("DATE");

            String result =
                    invokeBuildSeqMessage(event);

            assertNotNull(result);

            assertTrue(
                    result.contains("Процес: ")
            );

            assertTrue(
                    result.contains("PID: 10")
            );

            assertTrue(
                    result.contains("50 %")
            );
        }
    }

    // ============================================================
    // NTP fallback
    // ============================================================

    @Test
    void buildSeqMessage_shouldUsePcTime_whenNtpReturnsNull()
            throws Exception {

        when(event.getProcessName())
                .thenReturn("test.exe");

        when(event.getPid())
                .thenReturn(100);

        when(event.getAction())
                .thenReturn("MUTE");

        when(event.getCurrentVolume())
                .thenReturn(0.4f);

        try (
                MockedConstruction<NTPTimeService> ignored =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(null)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.0.10");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            /*
             * Date создаётся через Instant.now(),
             * поэтому точное значение нам неизвестно.
             */
            formatterMock.when(() ->
                            DateFormatter.dateConvert(
                                    any(Date.class)
                            )
                    )
                    .thenReturn("PC_DATE");

            String result =
                    invokeBuildSeqMessage(event);

            assertTrue(
                    result.contains("PC_DATE")
            );

            /*
             * TIME_PC у вас является константой.
             * Если хотите, здесь можно сделать ещё
             * assertTrue(result.contains(TIME_PC));
             *
             * если константа доступна из теста.
             */
        }
    }

    // ============================================================
    // logAudioEvent()
    // ============================================================

    @Test
    void logAudioEvent_shouldSendGeneratedMessageToLogger()
            throws Exception {

        Date date = new Date(1000);

        when(event.getProcessName())
                .thenReturn("music.exe");

        when(event.getPid())
                .thenReturn(321);

        when(event.getAction())
                .thenReturn("MUTED");

        when(event.getCurrentVolume())
                .thenReturn(0.7f);

        try (
                MockedConstruction<NTPTimeService> ignored =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(date)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("10.0.0.5");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("admin");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(date)
                    )
                    .thenReturn("DATE");

            invokeLogAudioEvent(event);

            verify(logger)
                    .log(
                            argThat((String message) ->
                                    message.contains("music.exe")
                                            && message.contains("321")
                                            && message.contains("MUTED")
                                            && message.contains("70 %")
                            )
                    );
        }
    }

    @Test
    void logAudioEvent_shouldNotThrow_whenLoggerThrowsException()
            throws Exception {

        Date date = new Date(1000);

        when(event.getProcessName())
                .thenReturn("test.exe");

        when(event.getAction())
                .thenReturn("MUTE");

        doThrow(
                new RuntimeException("Seq unavailable")
        ).when(logger).log(anyString());

        try (
                MockedConstruction<NTPTimeService> ignored =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(date)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("127.0.0.1");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(date)
                    )
                    .thenReturn("DATE");

            assertDoesNotThrow(
                    () -> invokeLogAudioEvent(event)
            );

            verify(logger)
                    .log(anyString());
        }
    }

    // ============================================================
    // stop()
    // ============================================================

    @Test
    void stop_shouldNotThrow_whenServerWasNeverStarted() {

        assertDoesNotThrow(
                server::stop
        );
    }

    // ============================================================
    // Reflection helpers
    // ============================================================

    private String invokeBuildSeqMessage(
            AudioEvent event
    ) throws Exception {

        Method method =
                AudioEventServer.class
                        .getDeclaredMethod(
                                "buildSeqMessage",
                                AudioEvent.class
                        );

        method.setAccessible(true);

        return (String) method.invoke(
                server,
                event
        );
    }

    private void invokeLogAudioEvent(
            AudioEvent event
    ) throws Exception {

        Method method =
                AudioEventServer.class
                        .getDeclaredMethod(
                                "logAudioEvent",
                                AudioEvent.class
                        );

        method.setAccessible(true);

        method.invoke(
                server,
                event
        );
    }
}
