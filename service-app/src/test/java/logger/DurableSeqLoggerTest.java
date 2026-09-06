package logger;

import org.windows_events.logger.DurableSeqLogger;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DurableSeqLoggerTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =====================================================================
    // Constructor
    // =====================================================================

    @Test
    void constructor_shouldCreateFile_whenFileDoesNotExist()
            throws Exception {

        Path logFile =
                tempDir.resolve("durable-seq.log");

        assertFalse(Files.exists(logFile));

        new DurableSeqLogger(
                "http://127.0.0.1:1",
                "test-api-key",
                logFile.toString()
        );

        assertTrue(Files.exists(logFile));
    }

    // =====================================================================
    // log()
    // =====================================================================

    @Test
    void log_shouldWriteMessageToFile_whenSeqIsUnavailable()
            throws Exception {

        Path logFile =
                tempDir.resolve("durable-seq.log");

        DurableSeqLogger logger =
                new DurableSeqLogger(
                        "http://127.0.0.1:1",
                        "test-api-key",
                        logFile.toString()
                );

        logger.log("Test event");

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(1, lines.size());
        assertEquals(
                "Test event",
                lines.get(0)
        );
    }

    @Test
    void log_shouldAppendMultipleMessagesToFile()
            throws Exception {

        Path logFile =
                tempDir.resolve("durable-seq.log");

        DurableSeqLogger logger =
                new DurableSeqLogger(
                        "http://127.0.0.1:1",
                        "test-api-key",
                        logFile.toString()
                );

        logger.log("First event");
        logger.log("Second event");
        logger.log("Third event");

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(3, lines.size());

        assertEquals(
                "First event",
                lines.get(0)
        );

        assertEquals(
                "Second event",
                lines.get(1)
        );

        assertEquals(
                "Third event",
                lines.get(2)
        );
    }

    // =====================================================================
    // Seq sending
    // =====================================================================

    @Test
    void logger_shouldSendMessageToSeq()
            throws Exception {

        AtomicReference<String> receivedBody =
                new AtomicReference<>();

        AtomicReference<String> receivedContentType =
                new AtomicReference<>();

        AtomicReference<String> receivedApiKey =
                new AtomicReference<>();

        AtomicInteger requestCount =
                new AtomicInteger();

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/api/events/raw",
                exchange -> {

                    requestCount.incrementAndGet();

                    receivedContentType.set(
                            exchange
                                    .getRequestHeaders()
                                    .getFirst("Content-Type")
                    );

                    receivedApiKey.set(
                            exchange
                                    .getRequestHeaders()
                                    .getFirst("X-Seq-ApiKey")
                    );

                    byte[] body =
                            exchange
                                    .getRequestBody()
                                    .readAllBytes();

                    receivedBody.set(
                            new String(
                                    body,
                                    StandardCharsets.UTF_8
                            )
                    );

                    exchange.sendResponseHeaders(
                            200,
                            -1
                    );

                    exchange.close();
                }
        );

        server.start();

        int port =
                server.getAddress().getPort();

        Path logFile =
                tempDir.resolve("seq.log");

        DurableSeqLogger logger =
                new DurableSeqLogger(
                        "http://127.0.0.1:" + port,
                        "my-secret-key",
                        logFile.toString()
                );

        logger.log("Camera disconnected");

        waitUntil(
                () -> requestCount.get() == 1,
                Duration.ofSeconds(4)
        );

        assertEquals(
                1,
                requestCount.get()
        );

        assertEquals(
                "application/vnd.serilog.clef",
                receivedContentType.get()
        );

        assertEquals(
                "my-secret-key",
                receivedApiKey.get()
        );

        assertNotNull(
                receivedBody.get()
        );

        assertTrue(
                receivedBody.get()
                        .contains("Camera disconnected")
        );

        assertTrue(
                receivedBody.get()
                        .contains("\"@t\"")
        );

        assertTrue(
                receivedBody.get()
                        .contains("\"@m\"")
        );
    }

    @Test
    void logger_shouldRemoveMessageFromFile_afterSuccessfulSend()
            throws Exception {

        AtomicInteger requestCount =
                new AtomicInteger();

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/api/events/raw",
                exchange -> {

                    requestCount.incrementAndGet();

                    exchange
                            .getRequestBody()
                            .readAllBytes();

                    exchange.sendResponseHeaders(
                            200,
                            -1
                    );

                    exchange.close();
                }
        );

        server.start();

        int port =
                server.getAddress().getPort();

        Path logFile =
                tempDir.resolve("seq.log");

        DurableSeqLogger logger =
                new DurableSeqLogger(
                        "http://127.0.0.1:" + port,
                        "api-key",
                        logFile.toString()
                );

        logger.log(
                "Event for successful sending"
        );

        /*
         * Ждём не только HTTP-запрос,
         * но и удаление строки из файла.
         */
        waitUntil(
                () ->
                        requestCount.get() >= 1 &&
                                Files.exists(logFile) &&
                                Files.readAllLines(logFile).isEmpty(),
                Duration.ofSeconds(4)
        );

        assertTrue(
                Files.readAllLines(logFile).isEmpty()
        );
    }

    // =====================================================================
    // Durable behaviour
    // =====================================================================

    @Test
    void constructor_shouldSendPreviouslyStoredEvent()
            throws Exception {

        AtomicReference<String> receivedBody =
                new AtomicReference<>();

        AtomicInteger requestCount =
                new AtomicInteger();

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/api/events/raw",
                exchange -> {

                    requestCount.incrementAndGet();

                    byte[] body =
                            exchange
                                    .getRequestBody()
                                    .readAllBytes();

                    receivedBody.set(
                            new String(
                                    body,
                                    StandardCharsets.UTF_8
                            )
                    );

                    exchange.sendResponseHeaders(
                            200,
                            -1
                    );

                    exchange.close();
                }
        );

        server.start();

        int port =
                server.getAddress().getPort();

        Path logFile =
                tempDir.resolve("old-events.log");

        /*
         * Событие было сохранено до запуска программы.
         */
        Files.writeString(
                logFile,
                "Event stored before restart"
                        + System.lineSeparator()
        );

        new DurableSeqLogger(
                "http://127.0.0.1:" + port,
                "api-key",
                logFile.toString()
        );

        waitUntil(
                () -> requestCount.get() >= 1,
                Duration.ofSeconds(3)
        );

        assertEquals(
                1,
                requestCount.get()
        );

        assertNotNull(
                receivedBody.get()
        );

        assertTrue(
                receivedBody.get()
                        .contains(
                                "Event stored before restart"
                        )
        );

        /*
         * После успешной отправки событие
         * должно исчезнуть из durable-файла.
         */
        waitUntil(
                () ->
                        Files.exists(logFile) &&
                                Files.readAllLines(logFile).isEmpty(),
                Duration.ofSeconds(2)
        );

        assertTrue(
                Files.readAllLines(logFile).isEmpty()
        );
    }

    @Test
    void logger_shouldKeepMessageInFile_whenSeqReturns500()
            throws Exception {

        AtomicInteger requestCount =
                new AtomicInteger();

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/api/events/raw",
                exchange -> {

                    requestCount.incrementAndGet();

                    exchange
                            .getRequestBody()
                            .readAllBytes();

                    exchange.sendResponseHeaders(
                            500,
                            -1
                    );

                    exchange.close();
                }
        );

        server.start();

        int port =
                server.getAddress().getPort();

        Path logFile =
                tempDir.resolve("failed.log");

        DurableSeqLogger logger =
                new DurableSeqLogger(
                        "http://127.0.0.1:" + port,
                        "api-key",
                        logFile.toString()
                );

        logger.log(
                "Must remain on disk"
        );

        waitUntil(
                () -> requestCount.get() >= 1,
                Duration.ofSeconds(4)
        );

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(
                1,
                lines.size()
        );

        assertEquals(
                "Must remain on disk",
                lines.get(0)
        );
    }

    // =====================================================================
    // JSON escaping
    // =====================================================================

    @Test
    void logger_shouldEscapeSpecialCharactersInMessage()
            throws Exception {

        AtomicReference<String> body =
                new AtomicReference<>();

        server = HttpServer.create(
                new InetSocketAddress(0),
                0
        );

        server.createContext(
                "/api/events/raw",
                exchange -> {

                    body.set(
                            new String(
                                    exchange
                                            .getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    );

                    exchange.sendResponseHeaders(
                            200,
                            -1
                    );

                    exchange.close();
                }
        );

        server.start();

        int port =
                server.getAddress().getPort();

        Path logFile =
                tempDir.resolve("json.log");

        DurableSeqLogger logger =
                new DurableSeqLogger(
                        "http://127.0.0.1:" + port,
                        "api-key",
                        logFile.toString()
                );

        logger.log(
                "User said \"Hello\""
        );

        waitUntil(
                () -> body.get() != null,
                Duration.ofSeconds(4)
        );

        assertNotNull(body.get());

        /*
         * Кавычки внутри @m должны быть escaped.
         */
        assertTrue(
                body.get()
                        .contains(
                                "User said \\\"Hello\\\""
                        )
        );
    }

    // =====================================================================
    // Helper
    // =====================================================================

    private void waitUntil(
            CheckedCondition condition,
            Duration timeout
    ) throws Exception {

        long deadline =
                System.nanoTime()
                        + timeout.toNanos();

        while (
                System.nanoTime() < deadline
        ) {

            if (condition.evaluate()) {
                return;
            }

            Thread.sleep(25);
        }

        fail(
                "Condition was not satisfied within "
                        + timeout
        );
    }

    @FunctionalInterface
    private interface CheckedCondition {

        boolean evaluate()
                throws Exception;
    }
}
