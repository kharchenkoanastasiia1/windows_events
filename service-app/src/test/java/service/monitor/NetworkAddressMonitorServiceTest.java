package service.monitor;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.windows_events.service.monitor.NetworkAddressMonitorService;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetworkAddressMonitorServiceTest {

    @Test
    void getHostName_shouldReturnHostName() throws Exception {

        InetAddress address = mock(InetAddress.class);

        when(address.getHostName())
                .thenReturn("PC-01");

        try (
                MockedStatic<InetAddress> inet =
                        mockStatic(InetAddress.class)
        ) {

            inet.when(InetAddress::getLocalHost)
                    .thenReturn(address);

            String result =
                    NetworkAddressMonitorService.getHostName();

            assertEquals(
                    "PC-01",
                    result
            );
        }
    }

    @Test
    void getHostName_shouldReturnEmptyString_whenExceptionOccurs()
            throws Exception {

        try (
                MockedStatic<InetAddress> inet =
                        mockStatic(InetAddress.class)
        ) {

            inet.when(InetAddress::getLocalHost)
                    .thenThrow(
                            new RuntimeException("Host error")
                    );

            String result =
                    NetworkAddressMonitorService.getHostName();

            assertEquals(
                    "",
                    result
            );
        }
    }

    @Test
    void getIpAddress_shouldReturnFirstNonLoopbackIPv4()
            throws Exception {

        NetworkInterface networkInterface =
                mock(NetworkInterface.class);

        Inet4Address loopback =
                mock(Inet4Address.class);

        Inet4Address validAddress =
                mock(Inet4Address.class);

        when(loopback.isLoopbackAddress())
                .thenReturn(true);

        when(validAddress.isLoopbackAddress())
                .thenReturn(false);

        when(validAddress.getHostAddress())
                .thenReturn("192.168.1.100");

        Enumeration<InetAddress> addresses =
                Collections.enumeration(
                        java.util.List.of(
                                loopback,
                                validAddress
                        )
                );

        when(networkInterface.getInetAddresses())
                .thenReturn(addresses);

        Enumeration<NetworkInterface> interfaces =
                Collections.enumeration(
                        java.util.List.of(
                                networkInterface
                        )
                );

        try (
                MockedStatic<NetworkInterface> network =
                        mockStatic(NetworkInterface.class)
        ) {

            network.when(
                            NetworkInterface::getNetworkInterfaces
                    )
                    .thenReturn(interfaces);

            String result =
                    NetworkAddressMonitorService.getIpAddress();

            assertEquals(
                    "192.168.1.100",
                    result
            );
        }
    }

    @Test
    void getIpAddress_shouldReturnEmptyString_whenNoSuitableAddress()
            throws Exception {

        NetworkInterface networkInterface =
                mock(NetworkInterface.class);

        Inet4Address loopback =
                mock(Inet4Address.class);

        when(loopback.isLoopbackAddress())
                .thenReturn(true);

        when(networkInterface.getInetAddresses())
                .thenReturn(
                        Collections.enumeration(
                                java.util.List.of(loopback)
                        )
                );

        try (
                MockedStatic<NetworkInterface> network =
                        mockStatic(NetworkInterface.class)
        ) {

            network.when(
                            NetworkInterface::getNetworkInterfaces
                    )
                    .thenReturn(
                            Collections.enumeration(
                                    java.util.List.of(
                                            networkInterface
                                    )
                            )
                    );

            assertEquals(
                    "",
                    NetworkAddressMonitorService.getIpAddress()
            );
        }
    }

    @Test
    void getIpAddress_shouldReturnEmptyString_whenSocketExceptionOccurs()
            throws Exception {

        try (
                MockedStatic<NetworkInterface> network =
                        mockStatic(NetworkInterface.class)
        ) {

            network.when(
                            NetworkInterface::getNetworkInterfaces
                    )
                    .thenThrow(
                            new SocketException(
                                    "Network error"
                            )
                    );

            assertEquals(
                    "",
                    NetworkAddressMonitorService.getIpAddress()
            );
        }
    }
}
