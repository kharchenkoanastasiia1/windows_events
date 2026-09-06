package service.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.numlock.NumLockEvent;
import org.windows_events.service.DateFormatter;
import org.windows_events.service.monitor.NetworkAddressMonitorService;
import org.windows_events.service.monitor.UserMonitorService;
import org.windows_events.service.server.NumLockEventServer;
import org.windows_events.time.NTPTimeService;

import java.lang.reflect.Method;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NumLockEventServerTest {

    private DurableSeqLogger logger;
    private NumLockEventServer server;

    @BeforeEach
    void setUp() {

        logger =
                mock(DurableSeqLogger.class);

        server =
                new NumLockEventServer(
                        logger,
                        44555,
                        "pool.ntp.org"
                );
    }

    // ============================================================
    // buildSeqMessage()
    // ============================================================

    @Test
    void buildSeqMessage_shouldBuildNumLockOnMessage()
            throws Exception {

        NumLockEvent event =
                mock(NumLockEvent.class);

        Date ntpDate =
                new Date(1000);

        when(event.getNumLockState())
                .thenReturn("ON");

        try (
                MockedConstruction<NTPTimeService> ignored =
                        mockConstruction(
                                NTPTimeService.class,
                                (mock, context) ->
                                        when(mock.getNTPTime())
                                                .thenReturn(ntpDate)
                        );

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(
                                NetworkAddressMonitorService.class
                        );

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(
                                UserMonitorService.class
                        );

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(
                                DateFormatter.class
                        )
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
                            DateFormatter.dateConvert(
                                    ntpDate
                            )
                    )
                    .thenReturn(
                            "06.09.2026 15:00"
                    );

            String result =
                    invokeBuildSeqMessage(event);

            assertNotNull(result);

            assertFalse(
                    result.isEmpty()
            );

            assertTrue(
                    result.contains(
                            "06.09.2026 15:00"
                    )
            );

            /*
             * Если NUMLOCK_ON доступен вашему
             * тестовому классу через static import,
             * лучше дополнительно:
             *
             * assertTrue(result.contains(NUMLOCK_ON));
             */
        }
    }

    @Test
    void buildSeqMessage_shouldBuildNumLockOffMessage()
            throws Exception {

        NumLockEvent event =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(event.getNumLockState())
                .thenReturn("OFF");

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
                    .thenReturn("192.168.1.1");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(date)
                    )
                    .thenReturn("DATE");

            String result =
                    invokeBuildSeqMessage(event);

            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    // ============================================================
    // NTP fallback
    // ============================================================

    @Test
    void buildSeqMessage_shouldUsePcTime_whenNtpReturnsNull()
            throws Exception {

        NumLockEvent event =
                mock(NumLockEvent.class);

        when(event.getNumLockState())
                .thenReturn("ON");

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
                    .thenReturn("10.0.0.1");

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            formatterMock.when(() ->
                            DateFormatter.dateConvert(
                                    any(Date.class)
                            )
                    )
                    .thenReturn("LOCAL_DATE");

            String result =
                    invokeBuildSeqMessage(event);

            assertTrue(
                    result.contains(
                            "LOCAL_DATE"
                    )
            );
        }
    }

    // ============================================================
    // Deduplication
    // ============================================================

    @Test
    void logNumLockEvent_shouldLogFirstEvent()
            throws Exception {

        NumLockEvent event =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(event.getNumLockState())
                .thenReturn("ON");

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

            prepareEnvironment(
                    networkMock,
                    userMock,
                    formatterMock,
                    date
            );

            invokeLogNumLockEvent(event);

            verify(logger, times(1))
                    .log(anyString());
        }
    }

    @Test
    void logNumLockEvent_shouldIgnoreDuplicateState()
            throws Exception {

        NumLockEvent first =
                mock(NumLockEvent.class);

        NumLockEvent second =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(first.getNumLockState())
                .thenReturn("ON");

        when(second.getNumLockState())
                .thenReturn("ON");

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

            prepareEnvironment(
                    networkMock,
                    userMock,
                    formatterMock,
                    date
            );

            invokeLogNumLockEvent(first);
            invokeLogNumLockEvent(second);

            /*
             * ON -> ON
             *
             * Второй ON должен быть проигнорирован.
             */
            verify(logger, times(1))
                    .log(anyString());
        }
    }

    @Test
    void logNumLockEvent_shouldLogWhenStateChanges()
            throws Exception {

        NumLockEvent on =
                mock(NumLockEvent.class);

        NumLockEvent off =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(on.getNumLockState())
                .thenReturn("ON");

        when(off.getNumLockState())
                .thenReturn("OFF");

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

            prepareEnvironment(
                    networkMock,
                    userMock,
                    formatterMock,
                    date
            );

            invokeLogNumLockEvent(on);
            invokeLogNumLockEvent(off);

            /*
             * ON -> OFF
             *
             * Это разные состояния.
             */
            verify(logger, times(2))
                    .log(anyString());
        }
    }

    @Test
    void logNumLockEvent_shouldLogOnOffOnSequence()
            throws Exception {

        NumLockEvent on1 =
                mock(NumLockEvent.class);

        NumLockEvent off =
                mock(NumLockEvent.class);

        NumLockEvent on2 =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(on1.getNumLockState())
                .thenReturn("ON");

        when(off.getNumLockState())
                .thenReturn("OFF");

        when(on2.getNumLockState())
                .thenReturn("ON");

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

            prepareEnvironment(
                    networkMock,
                    userMock,
                    formatterMock,
                    date
            );

            invokeLogNumLockEvent(on1);
            invokeLogNumLockEvent(off);
            invokeLogNumLockEvent(on2);

            verify(logger, times(3))
                    .log(anyString());
        }
    }

    @Test
    void logNumLockEvent_shouldIgnoreSecondOffEvent()
            throws Exception {

        NumLockEvent on =
                mock(NumLockEvent.class);

        NumLockEvent off1 =
                mock(NumLockEvent.class);

        NumLockEvent off2 =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(on.getNumLockState())
                .thenReturn("ON");

        when(off1.getNumLockState())
                .thenReturn("OFF");

        when(off2.getNumLockState())
                .thenReturn("OFF");

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

            prepareEnvironment(
                    networkMock,
                    userMock,
                    formatterMock,
                    date
            );

            invokeLogNumLockEvent(on);
            invokeLogNumLockEvent(off1);
            invokeLogNumLockEvent(off2);

            /*
             * ON -> OFF -> OFF
             *
             * Последний OFF не логируется.
             */
            verify(logger, times(2))
                    .log(anyString());
        }
    }

    // ============================================================
    // logger exception
    // ============================================================

    @Test
    void logNumLockEvent_shouldNotThrow_whenLoggerThrows()
            throws Exception {

        NumLockEvent event =
                mock(NumLockEvent.class);

        Date date =
                new Date(1000);

        when(event.getNumLockState())
                .thenReturn("ON");

        doThrow(
                new RuntimeException(
                        "Seq unavailable"
                )
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

            prepareEnvironment(
                    networkMock,
                    userMock,
                    formatterMock,
                    date
            );

            assertDoesNotThrow(
                    () ->
                            invokeLogNumLockEvent(
                                    event
                            )
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
    // Helpers
    // ============================================================

    private String invokeBuildSeqMessage(
            NumLockEvent event
    ) throws Exception {

        Method method =
                NumLockEventServer.class
                        .getDeclaredMethod(
                                "buildSeqMessage",
                                NumLockEvent.class
                        );

        method.setAccessible(true);

        return (String) method.invoke(
                server,
                event
        );
    }

    private void invokeLogNumLockEvent(
            NumLockEvent event
    ) throws Exception {

        Method method =
                NumLockEventServer.class
                        .getDeclaredMethod(
                                "logNumLockEvent",
                                NumLockEvent.class
                        );

        method.setAccessible(true);

        method.invoke(
                server,
                event
        );
    }

    private void prepareEnvironment(
            MockedStatic<NetworkAddressMonitorService> networkMock,
            MockedStatic<UserMonitorService> userMock,
            MockedStatic<DateFormatter> formatterMock,
            Date date
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
                .thenReturn("user");

        formatterMock.when(() ->
                        DateFormatter.dateConvert(date)
                )
                .thenReturn("DATE");
    }
}
