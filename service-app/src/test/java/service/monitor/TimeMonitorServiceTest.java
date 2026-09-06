package service.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.service.DateFormatter;
import org.windows_events.service.monitor.NetworkAddressMonitorService;
import org.windows_events.service.monitor.TimeMonitorService;
import org.windows_events.service.monitor.UserMonitorService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TimeMonitorServiceTest {

    private DurableSeqLogger logger;
    private Runtime runtime;
    private Process process;

    @BeforeEach
    void setUp() {

        logger =
                mock(DurableSeqLogger.class);

        runtime =
                mock(Runtime.class);

        process =
                mock(Process.class);
    }

    @Test
    void setTimePC_shouldLogSuccessfulChange_whenCommandSucceeds()
            throws Exception {

        long systemTime = 1788690000000L;
        long ntpTime = 1788690600000L;

        when(runtime.exec(anyString()))
                .thenReturn(process);

        when(process.waitFor())
                .thenReturn(0);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(NetworkAddressMonitorService.class);

                MockedStatic<DateFormatter> formatterMock =
                        mockStatic(DateFormatter.class)
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            userMock.when(UserMonitorService::getActiveUser)
                    .thenReturn("test-user");

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC-01");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.100");

            formatterMock.when(() ->
                            DateFormatter.dateConvertString(systemTime)
                    )
                    .thenReturn("OLD_TIME");

            formatterMock.when(() ->
                            DateFormatter.dateConvertString(ntpTime)
                    )
                    .thenReturn("NEW_TIME");

            TimeMonitorService service =
                    new TimeMonitorService();

            service.setTimePC(
                    systemTime,
                    ntpTime,
                    logger
            );

            ArgumentCaptor<String> commandCaptor =
                    ArgumentCaptor.forClass(String.class);

            verify(runtime)
                    .exec(commandCaptor.capture());

            String command =
                    commandCaptor.getValue();

            assertNotNull(command);

            assertTrue(
                    command.contains("powershell")
            );

            assertTrue(
                    command.contains("Set-Date")
            );

            verify(process)
                    .waitFor();

            verify(logger)
                    .log(
                            argThat((String message) ->
                                    message.contains("PC-01")
                                            && message.contains("192.168.1.100")
                                            && message.contains("test-user")
                                            && message.contains("OLD_TIME")
                                            && message.contains("NEW_TIME")
                            )
                    );
        }
    }

    @Test
    void setTimePC_shouldLogErrorExitCode_whenProcessFails()
            throws Exception {

        when(runtime.exec(anyString()))
                .thenReturn(process);

        when(process.waitFor())
                .thenReturn(5);

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(
                                NetworkAddressMonitorService.class
                        )
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("127.0.0.2");

            new TimeMonitorService()
                    .setTimePC(
                            1000,
                            2000,
                            logger
                    );

            verify(logger)
                    .log(
                            argThat(message ->
                                    message.contains("5")
                            )
                    );
        }
    }

    @Test
    void setTimePC_shouldLogException_whenExecThrowsIOException()
            throws Exception {

        when(runtime.exec(anyString()))
                .thenThrow(
                        new IOException(
                                "PowerShell unavailable"
                        )
                );

        try (
                MockedStatic<Runtime> runtimeMock =
                        mockStatic(Runtime.class);

                MockedStatic<UserMonitorService> userMock =
                        mockStatic(UserMonitorService.class);

                MockedStatic<NetworkAddressMonitorService> networkMock =
                        mockStatic(
                                NetworkAddressMonitorService.class
                        )
        ) {

            runtimeMock.when(Runtime::getRuntime)
                    .thenReturn(runtime);

            userMock.when(
                            UserMonitorService::getActiveUser
                    )
                    .thenReturn("user");

            networkMock.when(
                            NetworkAddressMonitorService::getHostName
                    )
                    .thenReturn("PC");

            networkMock.when(
                            NetworkAddressMonitorService::getIpAddress
                    )
                    .thenReturn("192.168.1.10");

            new TimeMonitorService()
                    .setTimePC(
                            1000,
                            2000,
                            logger
                    );

            verify(logger)
                    .log(
                            argThat(message ->
                                    message.contains(
                                            "PowerShell unavailable"
                                    )
                            )
                    );
        }
    }
}
