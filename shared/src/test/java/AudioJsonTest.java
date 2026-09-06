import org.junit.jupiter.api.Test;
import org.windows_events.audio.AudioEvent;
import org.windows_events.audio.AudioJson;

import static org.junit.jupiter.api.Assertions.*;

class AudioJsonTest {

    @Test
    void toJson_shouldSerializeAllFields() {

        AudioEvent event =
                new AudioEvent(
                        "chrome.exe",
                        1234,
                        "MUTE",
                        true,
                        0.25f,
                        1788700000000L
                );

        String json =
                AudioJson.toJson(event);

        assertNotNull(json);

        assertTrue(
                json.contains(
                        "\"processName\":\"chrome.exe\""
                )
        );

        assertTrue(
                json.contains(
                        "\"pid\":1234"
                )
        );

        assertTrue(
                json.contains(
                        "\"action\":\"MUTE\""
                )
        );

        assertTrue(
                json.contains(
                        "\"volumeRestoredToMax\":true"
                )
        );

        assertTrue(
                json.contains(
                        "\"currentVolume\":0.25"
                )
        );

        assertTrue(
                json.contains(
                        "\"timestamp\":1788700000000"
                )
        );
    }

    @Test
    void toJson_shouldEscapeQuotes() {

        AudioEvent event =
                new AudioEvent(
                        "program \"test\".exe",
                        1,
                        "MUTE",
                        false,
                        0.5f,
                        100L
                );

        String json =
                AudioJson.toJson(event);

        assertTrue(
                json.contains(
                        "program \\\"test\\\".exe"
                )
        );
    }

    @Test
    void toJson_shouldEscapeBackslashes() {

        AudioEvent event =
                new AudioEvent(
                        "C:\\Program Files\\app.exe",
                        1,
                        "MUTE",
                        false,
                        0.5f,
                        100L
                );

        String json =
                AudioJson.toJson(event);

        assertTrue(
                json.contains(
                        "C:\\\\Program Files\\\\app.exe"
                )
        );
    }

    @Test
    void toJson_shouldSerializeNullStringsAsEmptyStrings() {

        AudioEvent event =
                new AudioEvent(
                        null,
                        1,
                        null,
                        false,
                        0.5f,
                        100L
                );

        String json =
                AudioJson.toJson(event);

        assertTrue(
                json.contains(
                        "\"processName\":\"\""
                )
        );

        assertTrue(
                json.contains(
                        "\"action\":\"\""
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
                        + "\"processName\":\"vlc.exe\","
                        + "\"pid\":999,"
                        + "\"action\":\"MUTE\","
                        + "\"volumeRestoredToMax\":true,"
                        + "\"currentVolume\":0.75,"
                        + "\"timestamp\":123456789"
                        + "}";

        AudioEvent event =
                AudioJson.fromJson(json);

        assertNotNull(event);

        assertEquals(
                "vlc.exe",
                event.getProcessName()
        );

        assertEquals(
                999,
                event.getPid()
        );

        assertEquals(
                "MUTE",
                event.getAction()
        );

        assertTrue(
                event.isVolumeRestoredToMax()
        );

        assertEquals(
                0.75f,
                event.getCurrentVolume(),
                0.0001f
        );

        assertEquals(
                123456789L,
                event.getTimestamp()
        );
    }

    @Test
    void fromJson_shouldUseDefaults_whenFieldsAreMissing() {

        AudioEvent event =
                AudioJson.fromJson(
                        "{}"
                );

        assertEquals(
                "",
                event.getProcessName()
        );

        assertEquals(
                0,
                event.getPid()
        );

        assertEquals(
                "",
                event.getAction()
        );

        assertFalse(
                event.isVolumeRestoredToMax()
        );

        assertEquals(
                0.0f,
                event.getCurrentVolume(),
                0.0001f
        );

        assertEquals(
                0L,
                event.getTimestamp()
        );
    }

    @Test
    void fromJson_shouldReadFalseBoolean() {

        String json =
                "{"
                        + "\"volumeRestoredToMax\":false"
                        + "}";

        AudioEvent event =
                AudioJson.fromJson(json);

        assertFalse(
                event.isVolumeRestoredToMax()
        );
    }

    @Test
    void fromJson_shouldReadNegativePid() {

        String json =
                "{"
                        + "\"pid\":-1"
                        + "}";

        AudioEvent event =
                AudioJson.fromJson(json);

        assertEquals(
                -1,
                event.getPid()
        );
    }

    @Test
    void fromJson_shouldReadFloatVolume() {

        String json =
                "{"
                        + "\"currentVolume\":0.123"
                        + "}";

        AudioEvent event =
                AudioJson.fromJson(json);

        assertEquals(
                0.123f,
                event.getCurrentVolume(),
                0.0001f
        );
    }

    @Test
    void fromJson_shouldReadEscapedQuoteInString() {

        String json =
                "{"
                        + "\"processName\":\"program \\\"test\\\".exe\","
                        + "\"pid\":1"
                        + "}";

        AudioEvent event =
                AudioJson.fromJson(json);

        assertEquals(
                "program \"test\".exe",
                event.getProcessName()
        );
    }

    // =========================================================
    // Round trip
    // =========================================================

    @Test
    void toJsonAndFromJson_shouldPreserveSimpleEvent() {

        AudioEvent original =
                new AudioEvent(
                        "chrome.exe",
                        1234,
                        "VOLUME_CHANGED",
                        true,
                        0.42f,
                        987654321L
                );

        String json =
                AudioJson.toJson(original);

        AudioEvent restored =
                AudioJson.fromJson(json);

        assertEquals(
                original.getProcessName(),
                restored.getProcessName()
        );

        assertEquals(
                original.getPid(),
                restored.getPid()
        );

        assertEquals(
                original.getAction(),
                restored.getAction()
        );

        assertEquals(
                original.isVolumeRestoredToMax(),
                restored.isVolumeRestoredToMax()
        );

        assertEquals(
                original.getCurrentVolume(),
                restored.getCurrentVolume(),
                0.0001f
        );

        assertEquals(
                original.getTimestamp(),
                restored.getTimestamp()
        );
    }
}