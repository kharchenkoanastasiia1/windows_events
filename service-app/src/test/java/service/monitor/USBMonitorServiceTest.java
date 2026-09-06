package service.monitor;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.windows_events.service.monitor.USBMonitorService;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.UsbDevice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class USBMonitorServiceTest {

    @Test
    void findUsbDevice_shouldAddUsbDevice()
            throws Exception {

        HardwareAbstractionLayer hal =
                mock(HardwareAbstractionLayer.class);

        UsbDevice usb =
                mock(UsbDevice.class);

        when(usb.getName())
                .thenReturn("USB Flash");

        when(usb.getVendor())
                .thenReturn("Kingston");

        when(usb.getUniqueDeviceId())
                .thenReturn("USB-123");

        when(usb.getConnectedDevices())
                .thenReturn(List.of());

        when(hal.getUsbDevices(true))
                .thenReturn(
                        List.of(usb)
                );

        try (
                MockedConstruction<SystemInfo> infos =
                        mockConstruction(
                                SystemInfo.class,
                                (mock, context) ->
                                        when(
                                                mock.getHardware()
                                        ).thenReturn(hal)
                        )
        ) {

            Set<String> devices =
                    new HashSet<>();

            new USBMonitorService()
                    .findUsbDevice(devices);

            assertEquals(
                    1,
                    devices.size()
            );

            assertTrue(
                    devices.contains(
                            "USB Flash | Kingston | USB-123"
                    )
            );
        }
    }

    @Test
    void findUsbDevice_shouldCollectChildDevices()
            throws Exception {

        HardwareAbstractionLayer hal =
                mock(HardwareAbstractionLayer.class);

        UsbDevice parent =
                mock(UsbDevice.class);

        UsbDevice child =
                mock(UsbDevice.class);

        when(parent.getName())
                .thenReturn("USB Hub");

        when(parent.getVendor())
                .thenReturn("Generic");

        when(parent.getUniqueDeviceId())
                .thenReturn("HUB-1");

        when(child.getName())
                .thenReturn("USB Mouse");

        when(child.getVendor())
                .thenReturn("Logitech");

        when(child.getUniqueDeviceId())
                .thenReturn("MOUSE-1");

        when(child.getConnectedDevices())
                .thenReturn(List.of());

        when(parent.getConnectedDevices())
                .thenReturn(
                        List.of(child)
                );

        when(hal.getUsbDevices(true))
                .thenReturn(
                        List.of(parent)
                );

        try (
                MockedConstruction<SystemInfo> ignored =
                        mockConstruction(
                                SystemInfo.class,
                                (mock, context) ->
                                        when(
                                                mock.getHardware()
                                        ).thenReturn(hal)
                        )
        ) {

            Set<String> devices =
                    new HashSet<>();

            new USBMonitorService()
                    .findUsbDevice(devices);

            assertEquals(
                    2,
                    devices.size()
            );

            assertTrue(
                    devices.contains(
                            "USB Hub | Generic | HUB-1"
                    )
            );

            assertTrue(
                    devices.contains(
                            "USB Mouse | Logitech | MOUSE-1"
                    )
            );
        }
    }

    @Test
    void findUsbDevice_shouldNotThrow_whenSystemInfoFails() {

        try (
                MockedConstruction<SystemInfo> ignored =
                        mockConstruction(
                                SystemInfo.class,
                                (mock, context) -> {
                                    when(
                                            mock.getHardware()
                                    ).thenThrow(
                                            new RuntimeException(
                                                    "OSHI error"
                                            )
                                    );
                                }
                        )
        ) {

            Set<String> devices =
                    new HashSet<>();

            assertDoesNotThrow(
                    () ->
                            new USBMonitorService()
                                    .findUsbDevice(
                                            devices
                                    )
            );

            assertTrue(
                    devices.isEmpty()
            );
        }
    }
}
