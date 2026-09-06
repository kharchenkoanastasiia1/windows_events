package time;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockedConstruction;
import org.windows_events.time.NTPTimeService;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NTPTimeServiceTest {

    @AfterEach
    void tearDown() {

        /*
         * На случай теста с interrupt().
         */
        Thread.interrupted();
    }

    // =====================================================================
    // Constructor / getter
    // =====================================================================

    @Test
    void constructor_shouldStoreNtpHost() {

        NTPTimeService service =
                new NTPTimeService(
                        "pool.ntp.org"
                );

        assertEquals(
                "pool.ntp.org",
                service.getNtpHost()
        );
    }

    // =====================================================================
    // Successful request
    // =====================================================================

    @Test
    void getNTPTime_shouldReturnDate_whenServerResponds()
            throws Exception {

        long expectedMillis =
                1788690600000L;

        TimeInfo timeInfo =
                mock(TimeInfo.class);

        NtpV3Packet packet =
                mock(NtpV3Packet.class);

        TimeStamp timeStamp =
                mock(TimeStamp.class);

        when(timeInfo.getMessage())
                .thenReturn(packet);

        when(packet.getTransmitTimeStamp())
                .thenReturn(timeStamp);

        when(timeStamp.getTime())
                .thenReturn(expectedMillis);

        try (
                MockedConstruction<NTPUDPClient> clients =
                        mockConstruction(
                                NTPUDPClient.class,
                                (client, context) -> {

                                    when(
                                            client.getTime(
                                                    any(InetAddress.class)
                                            )
                                    ).thenReturn(
                                            timeInfo
                                    );
                                }
                        )
        ) {

            NTPTimeService service =
                    new NTPTimeService(
                            "localhost"
                    );

            Date result =
                    service.getNTPTime();

            assertNotNull(result);

            assertEquals(
                    expectedMillis,
                    result.getTime()
            );

            assertEquals(
                    1,
                    clients.constructed().size()
            );

            NTPUDPClient client =
                    clients.constructed().get(0);

            verify(client)
                    .setDefaultTimeout(5000);

            verify(client)
                    .open();

            verify(client)
                    .getTime(
                            any(InetAddress.class)
                    );

            verify(timeInfo)
                    .computeDetails();

            verify(timeInfo)
                    .getMessage();

            verify(packet)
                    .getTransmitTimeStamp();

            /*
             * finally должен закрыть клиент.
             */
            verify(client)
                    .close();
        }
    }

    // =====================================================================
    // Client closing
    // =====================================================================

    @Test
    void getNTPTime_shouldCloseClient_whenRequestFails()
            throws Exception {

        try (
                MockedConstruction<NTPUDPClient> clients =
                        mockConstruction(
                                NTPUDPClient.class,
                                (client, context) -> {

                                    when(
                                            client.getTime(
                                                    any(InetAddress.class)
                                            )
                                    ).thenThrow(
                                            new IOException(
                                                    "NTP unavailable"
                                            )
                                    );
                                }
                        )
        ) {

            NTPTimeService service =
                    new NTPTimeService(
                            "localhost"
                    );

            /*
             * После первой ошибки production-код делает:
             *
             * Thread.sleep(3000)
             *
             * interrupt позволяет не ждать 3 секунды.
             *
             * В данном тесте нас интересует finally -> close().
             */
            Thread.currentThread().interrupt();

            assertThrows(
                    InterruptedException.class,
                    service::getNTPTime
            );

            assertEquals(
                    1,
                    clients.constructed().size()
            );

            NTPUDPClient client =
                    clients.constructed().get(0);

            verify(client)
                    .open();

            verify(client)
                    .close();
        }
    }

    // =====================================================================
    // Retry
    // =====================================================================

    @Test
    void getNTPTime_shouldRetryAndReturnDate_afterFirstFailure()
            throws Exception {

        long expectedMillis =
                1788690600000L;

        TimeInfo timeInfo =
                mock(TimeInfo.class);

        NtpV3Packet packet =
                mock(NtpV3Packet.class);

        TimeStamp timeStamp =
                mock(TimeStamp.class);

        when(timeInfo.getMessage())
                .thenReturn(packet);

        when(packet.getTransmitTimeStamp())
                .thenReturn(timeStamp);

        when(timeStamp.getTime())
                .thenReturn(expectedMillis);

        try (
                MockedConstruction<NTPUDPClient> clients =
                        mockConstruction(
                                NTPUDPClient.class,
                                (client, context) -> {

                                    /*
                                     * Первый new NTPUDPClient()
                                     */
                                    if (
                                            context.getCount() == 1
                                    ) {

                                        when(
                                                client.getTime(
                                                        any(
                                                                InetAddress.class
                                                        )
                                                )
                                        ).thenThrow(
                                                new IOException(
                                                        "First attempt failed"
                                                )
                                        );

                                    } else {

                                        /*
                                         * Второй экземпляр клиента
                                         * уже успешно отвечает.
                                         */
                                        when(
                                                client.getTime(
                                                        any(
                                                                InetAddress.class
                                                        )
                                                )
                                        ).thenReturn(
                                                timeInfo
                                        );
                                    }
                                }
                        )
        ) {

            NTPTimeService service =
                    new NTPTimeService(
                            "localhost"
                    );

            Date result =
                    service.getNTPTime();

            assertNotNull(result);

            assertEquals(
                    expectedMillis,
                    result.getTime()
            );

            /*
             * После первой ошибки должен быть создан
             * второй NTPUDPClient.
             */
            assertEquals(
                    2,
                    clients.constructed().size()
            );

            NTPUDPClient firstClient =
                    clients.constructed().get(0);

            NTPUDPClient secondClient =
                    clients.constructed().get(1);

            verify(firstClient)
                    .close();

            verify(secondClient)
                    .close();

            verify(firstClient)
                    .getTime(
                            any(InetAddress.class)
                    );

            verify(secondClient)
                    .getTime(
                            any(InetAddress.class)
                    );
        }
    }

    // =====================================================================
    // All attempts fail
    // =====================================================================

    @Test
    void getNTPTime_shouldReturnNull_afterFiveFailedAttempts()
            throws Exception {

        try (
                MockedConstruction<NTPUDPClient> clients =
                        mockConstruction(
                                NTPUDPClient.class,
                                (client, context) -> {

                                    when(
                                            client.getTime(
                                                    any(InetAddress.class)
                                            )
                                    ).thenThrow(
                                            new IOException(
                                                    "NTP unavailable"
                                            )
                                    );
                                }
                        )
        ) {

            NTPTimeService service =
                    new NTPTimeService(
                            "localhost"
                    );

            Date result =
                    service.getNTPTime();

            assertNull(result);

            /*
             * maxAttempts = 5
             */
            assertEquals(
                    5,
                    clients.constructed().size()
            );

            for (
                    NTPUDPClient client :
                    clients.constructed()
            ) {

                verify(client)
                        .setDefaultTimeout(5000);

                verify(client)
                        .open();

                verify(client)
                        .getTime(
                                any(InetAddress.class)
                        );

                verify(client)
                        .close();
            }
        }
    }
}
