package org.windows_events.service.monitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.service.DateFormatter;
import org.windows_events.time.NTPTimeService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.windows_events.constants.Constants.*;

public class NetworkConnectionSpeedMonitorService {
    private long THRESHOLD_MBPS = 100;
    private DurableSeqLogger durableLogger;

    public NetworkConnectionSpeedMonitorService(DurableSeqLogger durableLogger) {
        this.durableLogger = durableLogger;
    }

    /**
     * Проверяет все адаптеры из Get-NetAdapter | Select Name, LinkSpeed
     * и возвращает список адаптеров, у которых скорость <= 100 Мбит/с.
     */
    public void findAdaptersWithLowSpeed(String hostNTP) throws Exception {
        NTPTimeService ntpTimeService = new NTPTimeService(hostNTP);
        boolean access = true;

        Date date = ntpTimeService.getNTPTime();
        if(date == null){
            date = Date.from(Instant.now());
            access = false;
        }

        for (AdapterSpeedInfo adapter : getAdapters()) {
            if (adapter.getSpeedMbps().isPresent() && adapter.getSpeedMbps().get() <= THRESHOLD_MBPS) {
//                durableLogger.log(IDENTIFIER_PROGRAM +
//                        String.format(IDENTIFIER_PC, DataPCMonitorService.getHostName()
//                                , DataPCMonitorService.getIpAddress(), UserMonitorService.getActiveUser())
//                        + DateFormatter.dateConvert(ntpTimeService.getNTPTime())
//                        + String.format(LOW_NETWORK_SPEED, adapter.name, adapter.rawLinkSpeed));

                StringBuilder stringBuilder = new StringBuilder(IDENTIFIER_PROGRAM +
                        String.format(IDENTIFIER_PC, NetworkAddressMonitorService.getHostName()
                                , NetworkAddressMonitorService.getIpAddress(), UserMonitorService.getActiveUser())
                        + DateFormatter.dateConvert(date));
                if (!access) {
                    stringBuilder.append(TIME_PC);
                }
                stringBuilder.append(String.format(LOW_NETWORK_SPEED, adapter.name, adapter.rawLinkSpeed));

                durableLogger.log(stringBuilder.toString());
            }
        }
    }

    private List<AdapterSpeedInfo> getAdapters() throws IOException, InterruptedException {
        String psCommand =
                "Get-NetAdapter | Select-Object Name, LinkSpeed | ConvertTo-Json -Compress";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                psCommand
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder json = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("PowerShell exited with code " + exitCode);
        }

        return parseAdapters(json.toString());
    }

    private List<AdapterSpeedInfo> parseAdapters(String json) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        List<AdapterSpeedInfo> result = new ArrayList<>();

        if (json == null || json.isBlank()) {
            return result;
        }

        JsonNode root = mapper.readTree(json);

        if (root.isArray()) {

            for (JsonNode node : root) {

                String name = node.path("Name").asText();
                String speed = node.path("LinkSpeed").asText();

                result.add(new AdapterSpeedInfo(
                        name,
                        speed,
                        parseSpeedToMbps(speed)
                ));
            }

        } else {

            // Если адаптер всего один, PowerShell возвращает объект,
            // а не массив.

            String name = root.path("Name").asText();
            String speed = root.path("LinkSpeed").asText();

            result.add(new AdapterSpeedInfo(
                    name,
                    speed,
                    parseSpeedToMbps(speed)
            ));
        }

        return result;
    }

    /**
     * Поддерживает значения вроде:
     * 10 Mbps
     * 100 Mbps
     * 1 Gbps
     * 2.5 Gbps
     */
    private Optional<Long> parseSpeedToMbps(String rawSpeed) {
        if (rawSpeed == null || rawSpeed.isBlank()) {
            return Optional.empty();
        }

//        Pattern pattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(Mbps|Gbps)", Pattern.CASE_INSENSITIVE);
        Pattern pattern = Pattern.compile(
                "([0-9]+(?:[.,][0-9]+)?)\\s*(bps|Mbps|Gbps)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(rawSpeed.trim());

        //TODO: (!matcher.find())
        if (!matcher.matches()) {
            return Optional.empty();
        }

        double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);

        double speedMbps;
        switch (unit) {
            case "gbps":
                speedMbps = value * 1000;
                break;

            case "mbps":
                speedMbps = value;
                break;

            case "bps":
                speedMbps = value / 1_000_000.0;
                break;

            default:
                return Optional.empty();
        }

//        if ("gbps".equals(unit)) {
//            speedMbps = value * 1000;
//        } else {
//            speedMbps = value;
//        }

        return Optional.of((long) speedMbps);
    }

    public static class AdapterSpeedInfo {
        private final String name;
        private final String rawLinkSpeed;
        private final Optional<Long> speedMbps;

        public AdapterSpeedInfo(String name, String rawLinkSpeed, Optional<Long> speedMbps) {
            this.name = name;
            this.rawLinkSpeed = rawLinkSpeed;
            this.speedMbps = speedMbps;
        }

        public String getName() {
            return name;
        }

        public String getRawLinkSpeed() {
            return rawLinkSpeed;
        }

        public Optional<Long> getSpeedMbps() {
            return speedMbps;
        }

        @Override
        public String toString() {
            return "AdapterSpeedInfo{" +
                    "name='" + name + '\'' +
                    ", rawLinkSpeed='" + rawLinkSpeed + '\'' +
                    ", speedMbps=" + speedMbps +
                    '}';
        }
    }
}
