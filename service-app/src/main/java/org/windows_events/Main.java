package org.windows_events;

import org.windows_events.constants.Constants;
import org.windows_events.controller.CheckServicesNeedScheduler;
import org.windows_events.controller.FirstRunController;
import org.windows_events.scheduler.TimeMonitorScheduler;
import org.windows_events.file.ConfigurationFile;
import org.windows_events.file.TimeShutdownFile;
import org.windows_events.logger.DurableSeqLogger;
import org.windows_events.scheduler.USBMonitorScheduler;
import org.windows_events.service.ShortcutUtil;
import org.windows_events.service.server.AudioEventServer;
import org.windows_events.service.server.NumLockEventServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.windows_events.constants.Constants.CHECK_NUMLOCK;
import static org.windows_events.constants.Constants.CHECK_SOUND_PROGRAM;

public class Main {

    public static void main(String[] args) throws Exception {
        Map<String, String> mapSeq = ConfigurationFile.readConfigurationSeq();

//        Log.setLogger(new LoggerConfiguration()
//                .writeTo(coloredConsole())
//                .writeTo(rollingFile("test-{Date}.log"), LogEventLevel.Information)
//                .writeTo(seq("http://localhost:5341/"))
//                .setMinimumLevel(LogEventLevel.Verbose)
//                .createLogger());

        DurableSeqLogger durableLogger =
                new DurableSeqLogger(
                        mapSeq.get(Constants.URL),
                        mapSeq.get(Constants.KEY),
                        "seq-buffer.log"
                );

        String hostNtp = ConfigurationFile.readTimeServerHost();

        Thread.sleep(1500);

        //Логирование времени включения/выключения
        FirstRunController controller = new FirstRunController(durableLogger);
        controller.loggingStartupAndShutdown(hostNtp);

        //Создание файла с разрешенными USB-устройствами (если его нет). Получение списка разрешенных устройств
        Set<String> currentUsbDevice = controller.firstRunService();
        USBMonitorScheduler monitor = new USBMonitorScheduler(durableLogger);
        monitor.setPreviousDevices(currentUsbDevice);

        //Инициализация шедулеров для контроля времени и прочего
        TimeMonitorScheduler timeMonitorScheduler = new TimeMonitorScheduler(durableLogger);
        CheckServicesNeedScheduler checkServicesNeedScheduler = new CheckServicesNeedScheduler(durableLogger, hostNtp);

        //Запуск процесса мониторинга за USB-устройствами
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(monitor);

        //Проверка необходимости контроля NumLock и выполнение в случае успеха
        if(checkServicesNeedScheduler.getMapSeq().get(CHECK_NUMLOCK)){
            Path baseDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            String workDirectory = baseDir.resolve("WindowsEventsNumLockAgent.exe").toAbsolutePath().toString();
            ShortcutUtil.createShortcut(workDirectory
                    , "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\StartUp\\WindowsEventsNumLockAgent.lnk"
                    , "WindowsEventsNumLockAgent");

            NumLockEventServer numLockEventServer = new NumLockEventServer(durableLogger, 44555, hostNtp);
            ExecutorService executorNumLock = Executors.newFixedThreadPool(1);
            executorNumLock.submit(numLockEventServer);
        }

        //Проверка необходимости контроля звука приложений и выполнение в случае успеха
        if(checkServicesNeedScheduler.getMapSeq().get(CHECK_SOUND_PROGRAM)){
            Path baseDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            String workDirectory = baseDir.resolve("WindowsEventsAudioAgent.exe").toAbsolutePath().toString();
            ShortcutUtil.createShortcut(workDirectory
                    , "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\StartUp\\WindowsEventsAudioAgent.lnk"
                    , "WindowsEventsAudioAgent");

            AudioEventServer audioEventServer = new AudioEventServer(durableLogger, 47632, hostNtp);
            ExecutorService executorAudio = Executors.newFixedThreadPool(3);
            executorAudio.submit(audioEventServer);

            Thread.sleep(1500);
        }

        //запуск контроля времени
        timeMonitorScheduler.run(hostNtp);

        //запуск контроля оставшихся процессов из раздела [Services]
        checkServicesNeedScheduler.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TimeShutdownFile timeShutdownFile = new TimeShutdownFile();
            try {
                timeShutdownFile.writeDateToFile(System.currentTimeMillis());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}