package org.windows_events.service.monitor.services_windows;

import org.windows_events.logger.DurableSeqLogger;

import static org.windows_events.constants.Constants.KEYBOARD_HOOK_SERVICE;


public class KeyboardHookMonitorService {

    public KeyboardHookMonitorService() {}

    public void check(DurableSeqLogger durableLogger, String hostNtp) throws Exception {
        CheckStartServicesWindows checkStartServicesWindows = new CheckStartServicesWindows(durableLogger, hostNtp);

        if(!checkStartServicesWindows.isServiceRunning(KEYBOARD_HOOK_SERVICE)){
            checkStartServicesWindows.startService(KEYBOARD_HOOK_SERVICE);
        }
        if(!checkStartServicesWindows.isServiceAutoStart(KEYBOARD_HOOK_SERVICE)){
            checkStartServicesWindows.setServiceAutoStart(KEYBOARD_HOOK_SERVICE);
        }
    }
}
