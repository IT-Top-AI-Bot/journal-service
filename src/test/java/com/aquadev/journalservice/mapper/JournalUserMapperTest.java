package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.response.JournalGroupResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.model.JournalUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalUserMapperTest {

    private final JournalUserMapper mapper = new JournalUserMapper();

    @Test
    void toEntity_success_mapsAllFields() {
        JournalUserResponse source = new JournalUserResponse(
                List.of(new JournalGroupResponse(1, true, 10L, "Group 10")),
                null, 1L, 10L, "Name", 0, 2, "Stream", 1, "Photo",
                List.of(), List.of(), null, 1, LocalDate.now(), (short) 20,
                Instant.now(), Instant.now(), true, "Form"
        );

        JournalUser entity = mapper.toEntity(source, 1L);

        assertThat(entity.getJournalUserId()).isEqualTo(1L);
        assertThat(entity.getStreamId()).isEqualByComparingTo(2);
        assertThat(entity.getStreamName()).isEqualTo("Stream");
        assertThat(entity.getFullName()).isEqualTo("Name");
        assertThat(entity.getJournalGroups()).hasSize(1);
        assertThat(entity.getJournalGroups().iterator().next().getJournalGroupId()).isEqualTo(10L);
    }

    @Test
    void toEntity_missingStreamId_throwsException() {
        JournalUserResponse source = new JournalUserResponse(
                List.of(new JournalGroupResponse(1, true, 10L, "Group 10")),
                null, 1L, 10L, "Name", 0, null, "Stream", 1, "Photo",
                List.of(), List.of(), null, 1, LocalDate.now(), (short) 20,
                Instant.now(), Instant.now(), true, "Form"
        );

        assertThatThrownBy(() -> mapper.toEntity(source, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("streamId");
    }

    @Test
    void toEntity_nullSource_throwsException() {
        assertThatThrownBy(() -> mapper.toEntity(null, 1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
