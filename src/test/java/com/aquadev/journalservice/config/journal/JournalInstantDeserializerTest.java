package com.aquadev.journalservice.config.journal;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalInstantDeserializerTest {

    private final JournalInstantDeserializer deserializer = new JournalInstantDeserializer(ZoneId.of("UTC"));

    @Test
    void deserialize_isoInstant() throws Exception {
        JsonParser parser = mock(JsonParser.class);
        when(parser.getValueAsString()).thenReturn("2025-01-01T10:00:00Z");

        Instant result = deserializer.deserialize(parser, mock(DeserializationContext.class));

        assertThat(result).isEqualTo(Instant.parse("2025-01-01T10:00:00Z"));
    }

    @Test
    void deserialize_journalFormat() throws Exception {
        JsonParser parser = mock(JsonParser.class);
        when(parser.getValueAsString()).thenReturn("2025-01-01 10:00:00");

        Instant result = deserializer.deserialize(parser, mock(DeserializationContext.class));

        assertThat(result).isEqualTo(Instant.parse("2025-01-01T10:00:00Z"));
    }

    @Test
    void deserialize_offsetDateTime() throws Exception {
        JsonParser parser = mock(JsonParser.class);
        when(parser.getValueAsString()).thenReturn("2025-01-01T10:00:00+02:00");

        Instant result = deserializer.deserialize(parser, mock(DeserializationContext.class));

        assertThat(result).isEqualTo(Instant.parse("2025-01-01T08:00:00Z"));
    }

    @Test
    void deserialize_null_returnsNull() throws Exception {
        JsonParser parser = mock(JsonParser.class);
        when(parser.getValueAsString()).thenReturn(null);

        Instant result = deserializer.deserialize(parser, mock(DeserializationContext.class));

        assertThat(result).isNull();
    }
}
