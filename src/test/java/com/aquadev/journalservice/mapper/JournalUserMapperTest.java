package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.response.JournalGroupResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.repository.JournalGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JournalUserMapperTest {

    @Mock
    JournalGroupRepository journalGroupRepository;

    @InjectMocks
    JournalUserMapper mapper;

    @Test
    void toEntity_success_mapsAllFields() {
        given(journalGroupRepository.findByJournalGroupId(10L)).willReturn(Optional.empty());

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
    void toEntity_groupAlreadyExistsInDb_reusesExistingEntity() {
        JournalGroup existingGroup = new JournalGroup();
        existingGroup.setJournalGroupId(10L);
        existingGroup.setName("Group 10");

        given(journalGroupRepository.findByJournalGroupId(10L)).willReturn(Optional.of(existingGroup));

        JournalUserResponse source = new JournalUserResponse(
                List.of(new JournalGroupResponse(1, true, 10L, "Group 10")),
                null, 1L, 10L, "Name", 0, 2, "Stream", 1, "Photo",
                List.of(), List.of(), null, 1, LocalDate.now(), (short) 20,
                Instant.now(), Instant.now(), true, "Form"
        );

        JournalUser entity = mapper.toEntity(source, 1L);

        assertThat(entity.getJournalGroups()).hasSize(1);
        assertThat(entity.getJournalGroups().iterator().next()).isSameAs(existingGroup);
        verify(journalGroupRepository).findByJournalGroupId(10L);
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
