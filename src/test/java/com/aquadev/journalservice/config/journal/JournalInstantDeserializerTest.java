package com.aquadev.journalservice.config.journal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalInstantDeserializerTest {

    private final JournalInstantDeserializer deserializer = new JournalInstantDeserializer(ZoneId.of("UTC"));

    @ParameterizedTest(name = "Should deserialize {0} to {1}")
    @MethodSource("provideDateStrings")
    void deserialize_variousFormats(String input, String expectedInstant) throws Exception {
        // Arrange
        JsonParser parser = mock(JsonParser.class);
        when(parser.getValueAsString()).thenReturn(input);
        DeserializationContext context = mock(DeserializationContext.class);

        // Act
        Instant result = deserializer.deserialize(parser, context);

        // Assert
        if (expectedInstant == null) {
            assertThat(result).isNull();
        } else {
            assertThat(result).isEqualTo(Instant.parse(expectedInstant));
        }
    }

    private static Stream<Arguments> provideDateStrings() {
        return Stream.of(
                // ISO Instant
                Arguments.of("2025-01-01T10:00:00Z", "2025-01-01T10:00:00Z"),
                // Journal Format (Custom)
                Arguments.of("2025-01-01 10:00:00", "2025-01-01T10:00:00Z"),
                // Offset Date Time
                Arguments.of("2025-01-01T10:00:00+02:00", "2025-01-01T08:00:00Z"),
                // Null case
                Arguments.of(null, null)
        );
    }
}
