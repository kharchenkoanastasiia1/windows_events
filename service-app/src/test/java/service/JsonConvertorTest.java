package service;

import org.junit.jupiter.api.Test;
import org.windows_events.service.JsonConvertor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonConvertorTest {

    @Test
    void escapeJson_shouldEscapeBackslash() {

        String result =
                JsonConvertor.escapeJson(
                        "C:\\Windows\\System32"
                );

        assertEquals(
                "C:\\\\Windows\\\\System32",
                result
        );
    }

    @Test
    void escapeJson_shouldEscapeDoubleQuotes() {

        String result =
                JsonConvertor.escapeJson(
                        "User said \"Hello\""
                );

        assertEquals(
                "User said \\\"Hello\\\"",
                result
        );
    }

    @Test
    void escapeJson_shouldEscapeNewLine() {

        String result =
                JsonConvertor.escapeJson(
                        "First\nSecond"
                );

        assertEquals(
                "First\\nSecond",
                result
        );
    }

    @Test
    void escapeJson_shouldEscapeCarriageReturn() {

        String result =
                JsonConvertor.escapeJson(
                        "First\rSecond"
                );

        assertEquals(
                "First\\rSecond",
                result
        );
    }

    @Test
    void escapeJson_shouldEscapeAllSpecialCharacters() {

        String input =
                "Path: C:\\Test\\file.txt\n"
                        + "Message: \"Hello\"\r";

        String expected =
                "Path: C:\\\\Test\\\\file.txt\\n"
                        + "Message: \\\"Hello\\\"\\r";

        String result =
                JsonConvertor.escapeJson(input);

        assertEquals(
                expected,
                result
        );
    }

    @Test
    void escapeJson_shouldReturnSameString_whenNoSpecialCharacters() {

        String input =
                "Simple message 123";

        String result =
                JsonConvertor.escapeJson(input);

        assertEquals(
                input,
                result
        );
    }

    @Test
    void escapeJson_shouldReturnEmptyString_whenInputIsEmpty() {

        assertEquals(
                "",
                JsonConvertor.escapeJson("")
        );
    }
}
