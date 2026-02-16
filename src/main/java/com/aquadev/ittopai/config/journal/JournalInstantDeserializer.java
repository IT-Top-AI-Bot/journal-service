package com.aquadev.ittopai.config.journal;


import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class JournalInstantDeserializer extends ValueDeserializer<Instant> {

    private static final DateTimeFormatter JOURNAL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ZoneId zoneId;

    public JournalInstantDeserializer(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException _) {
            // Not an ISO-8601 instant; try parsing as an offset date-time.
        }

        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException _) {
            // No offset present; try parsing with the journal's local date-time format.
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, JOURNAL_FORMAT);
            return dateTime.atZone(zoneId).toInstant();
        } catch (DateTimeParseException _) {
            throw InvalidFormatException.from(parser, "Cannot parse Instant: " + value, value, Instant.class);
        }
    }
}
