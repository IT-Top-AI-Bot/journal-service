package com.aquadev.journalservice.dto.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class JournalHomeworkStatusTest {

    @ParameterizedTest
    @CsvSource({
            "0,  EXPIRED",
            "1,  CHECKED",
            "2,  IN_PROGRESS",
            "3,  NOT_COMPLETED",
            "4,  CHECKED_FINAL",
            "5,  DELETED_BY_TEACHER"
    })
    void fromId_knownIds_returnCorrectStatus(int id, String expectedName) {
        JournalHomeworkStatus status = JournalHomeworkStatus.fromId(id);
        assertThat(status.name()).isEqualTo(expectedName);
    }

    @Test
    void fromId_null_returnsUnknown() {
        assertThat(JournalHomeworkStatus.fromId(null)).isEqualTo(JournalHomeworkStatus.UNKNOWN);
    }

    @Test
    void fromId_unknownId_returnsUnknown() {
        assertThat(JournalHomeworkStatus.fromId(999)).isEqualTo(JournalHomeworkStatus.UNKNOWN);
    }

    @Test
    void fromId_negativeUnknown_returnsUnknown() {
        assertThat(JournalHomeworkStatus.fromId(-99)).isEqualTo(JournalHomeworkStatus.UNKNOWN);
    }

    @Test
    void unknownStatus_hasNegativeId() {
        assertThat(JournalHomeworkStatus.UNKNOWN.getId()).isEqualTo(-1);
    }

    @Test
    void allKnownStatuses_haveDifferentIds() {
        JournalHomeworkStatus[] values = JournalHomeworkStatus.values();
        long distinctIds = java.util.Arrays.stream(values)
                .map(JournalHomeworkStatus::getId)
                .distinct()
                .count();
        assertThat(distinctIds).isEqualTo(values.length);
    }
}
