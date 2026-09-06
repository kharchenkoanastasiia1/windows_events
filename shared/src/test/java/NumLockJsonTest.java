
import org.junit.jupiter.api.Test;
import org.windows_events.numlock.NumLockEvent;
import org.windows_events.numlock.NumLockJson;

import static org.junit.jupiter.api.Assertions.*;

class NumLockJsonTest {

    @Test
    void toJson_shouldSerializeAllFields() {

        NumLockEvent event = new NumLockEvent();

        event.setEventType("NUMLOCK_CHANGED");
        event.setUsername("test-user");
        event.setNumLockState("ON");
        event.setWindowTitle("Notepad");
        event.setProcessName("notepad.exe");
        event.setPid(1234);
        event.setTimestamp(1788700000000L);
        event.setHost("PC-01");
        event.setMessage("NumLock enabled");

        String json =
                NumLockJson.toJson(event);

        assertNotNull(json);

        assertTrue(
                json.contains(
                        "\"eventType\":\"NUMLOCK_CHANGED\""
                )
        );

        assertTrue(
                json.contains(
                        "\"username\":\"test-user\""
                )
        );

        assertTrue(
                json.contains(
                        "\"numLockState\":\"ON\""
                )
        );

        assertTrue(
                json.contains(
                        "\"windowTitle\":\"Notepad\""
                )
        );

        assertTrue(
                json.contains(
                        "\"processName\":\"notepad.exe\""
                )
        );

        assertTrue(
                json.contains(
                        "\"pid\":1234"
                )
        );

        assertTrue(
                json.contains(
                        "\"timestamp\":1788700000000"
                )
        );

        assertTrue(
                json.contains(
                        "\"host\":\"PC-01\""
                )
        );

        assertTrue(
                json.contains(
                        "\"message\":\"NumLock enabled\""
                )
        );
    }

    @Test
    void toJson_shouldEscapeQuotes() {

        NumLockEvent event =
                createDefaultEvent();

        event.setWindowTitle(
                "Window \"Test\""
        );

        String json =
                NumLockJson.toJson(event);

        assertTrue(
                json.contains(
                        "Window \\\"Test\\\""
                )
        );
    }

    @Test
    void toJson_shouldEscapeBackslashes() {

        NumLockEvent event =
                createDefaultEvent();

        event.setProcessName(
                "C:\\Windows\\app.exe"
        );

        String json =
                NumLockJson.toJson(event);

        assertTrue(
                json.contains(
                        "C:\\\\Windows\\\\app.exe"
                )
        );
    }

    @Test
    void toJson_shouldEscapeNewLineAndCarriageReturn() {

        NumLockEvent event =
                createDefaultEvent();

        event.setMessage(
                "First line\nSecond line\rThird"
        );

        String json =
                NumLockJson.toJson(event);

        assertTrue(
                json.contains(
                        "First line\\nSecond line\\rThird"
                )
        );
    }

    @Test
    void toJson_shouldSerializeNullStringsAsEmptyStrings() {

        NumLockEvent event =
                new NumLockEvent();

        event.setPid(1);
        event.setTimestamp(100L);

        String json =
                NumLockJson.toJson(event);

        assertTrue(
                json.contains(
                        "\"eventType\":\"\""
                )
        );

        assertTrue(
                json.contains(
                        "\"username\":\"\""
                )
        );

        assertTrue(
                json.contains(
                        "\"message\":\"\""
                )
        );
    }

    // =========================================================
    // fromJson()
    // =========================================================

    @Test
    void fromJson_shouldDeserializeAllFields() {

        String json =
                "{"
                        + "\"eventType\":\"NUMLOCK_CHANGED\","
                        + "\"username\":\"admin\","
                        + "\"numLockState\":\"OFF\","
                        + "\"windowTitle\":\"Notepad\","
                        + "\"processName\":\"notepad.exe\","
                        + "\"pid\":555,"
                        + "\"timestamp\":123456789,"
                        + "\"host\":\"PC-01\","
                        + "\"message\":\"NumLock disabled\""
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertNotNull(event);

        assertEquals(
                "NUMLOCK_CHANGED",
                event.getEventType()
        );

        assertEquals(
                "admin",
                event.getUsername()
        );

        assertEquals(
                "OFF",
                event.getNumLockState()
        );

        assertEquals(
                "Notepad",
                event.getWindowTitle()
        );

        assertEquals(
                "notepad.exe",
                event.getProcessName()
        );

        assertEquals(
                555,
                event.getPid()
        );

        assertEquals(
                123456789L,
                event.getTimestamp()
        );

        assertEquals(
                "PC-01",
                event.getHost()
        );

        assertEquals(
                "NumLock disabled",
                event.getMessage()
        );
    }

    @Test
    void fromJson_shouldHandleCommaInsideString() {

        String json =
                "{"
                        + "\"eventType\":\"TEST\","
                        + "\"windowTitle\":\"One, Two, Three\","
                        + "\"pid\":1"
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                "One, Two, Three",
                event.getWindowTitle()
        );
    }

    @Test
    void fromJson_shouldHandleColonInsideString() {

        String json =
                "{"
                        + "\"message\":\"Error: something happened\","
                        + "\"pid\":1"
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                "Error: something happened",
                event.getMessage()
        );
    }

    @Test
    void fromJson_shouldUnescapeQuotes() {

        String json =
                "{"
                        + "\"windowTitle\":\"Window \\\"Test\\\"\""
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                "Window \"Test\"",
                event.getWindowTitle()
        );
    }

    @Test
    void fromJson_shouldUnescapeBackslashes() {

        String json =
                "{"
                        + "\"processName\":\"C:\\\\Windows\\\\app.exe\""
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                "C:\\Windows\\app.exe",
                event.getProcessName()
        );
    }

    @Test
    void fromJson_shouldUnescapeNewLines() {

        String json =
                "{"
                        + "\"message\":\"First\\nSecond\\rThird\""
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                "First\nSecond\rThird",
                event.getMessage()
        );
    }

    @Test
    void fromJson_shouldUseDefaults_whenFieldsAreMissing() {

        NumLockEvent event =
                NumLockJson.fromJson(
                        "{}"
                );

        assertEquals(
                "",
                event.getEventType()
        );

        assertEquals(
                "",
                event.getUsername()
        );

        assertEquals(
                "",
                event.getNumLockState()
        );

        assertEquals(
                "",
                event.getWindowTitle()
        );

        assertEquals(
                "",
                event.getProcessName()
        );

        assertEquals(
                "",
                event.getHost()
        );

        assertEquals(
                "",
                event.getMessage()
        );

        assertEquals(
                -1,
                event.getPid()
        );

        assertEquals(
                0L,
                event.getTimestamp()
        );
    }

    @Test
    void fromJson_shouldSetPidMinusOne_whenPidIsInvalid() {

        String json =
                "{"
                        + "\"pid\":\"abc\""
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                -1,
                event.getPid()
        );
    }

    @Test
    void fromJson_shouldSetTimestampZero_whenTimestampIsInvalid() {

        String json =
                "{"
                        + "\"timestamp\":\"invalid\""
                        + "}";

        NumLockEvent event =
                NumLockJson.fromJson(json);

        assertEquals(
                0L,
                event.getTimestamp()
        );
    }

    @Test
    void fromJson_shouldHandleNullJson() {

        NumLockEvent event =
                NumLockJson.fromJson(null);

        assertNotNull(event);

        assertEquals(
                "",
                event.getEventType()
        );

        assertEquals(
                -1,
                event.getPid()
        );

        assertEquals(
                0L,
                event.getTimestamp()
        );
    }

    // =========================================================
    // Round trip
    // =========================================================

    @Test
    void toJsonAndFromJson_shouldPreserveEvent() {

        NumLockEvent original =
                new NumLockEvent();

        original.setEventType(
                "NUMLOCK_CHANGED"
        );

        original.setUsername(
                "user"
        );

        original.setNumLockState(
                "ON"
        );

        original.setWindowTitle(
                "Window \"Test\", application"
        );

        original.setProcessName(
                "C:\\Apps\\app.exe"
        );

        original.setPid(
                987
        );

        original.setTimestamp(
                123456789L
        );

        original.setHost(
                "PC-01"
        );

        original.setMessage(
                "First\nSecond"
        );

        String json =
                NumLockJson.toJson(original);

        NumLockEvent restored =
                NumLockJson.fromJson(json);

        assertEquals(
                original.getEventType(),
                restored.getEventType()
        );

        assertEquals(
                original.getUsername(),
                restored.getUsername()
        );

        assertEquals(
                original.getNumLockState(),
                restored.getNumLockState()
        );

        assertEquals(
                original.getWindowTitle(),
                restored.getWindowTitle()
        );

        assertEquals(
                original.getProcessName(),
                restored.getProcessName()
        );

        assertEquals(
                original.getPid(),
                restored.getPid()
        );

        assertEquals(
                original.getTimestamp(),
                restored.getTimestamp()
        );

        assertEquals(
                original.getHost(),
                restored.getHost()
        );

        assertEquals(
                original.getMessage(),
                restored.getMessage()
        );
    }

    private NumLockEvent createDefaultEvent() {

        NumLockEvent event =
                new NumLockEvent();

        event.setEventType("TEST");
        event.setUsername("user");
        event.setNumLockState("ON");
        event.setWindowTitle("Window");
        event.setProcessName("app.exe");
        event.setPid(1);
        event.setTimestamp(100L);
        event.setHost("PC");
        event.setMessage("message");

        return event;
    }
}