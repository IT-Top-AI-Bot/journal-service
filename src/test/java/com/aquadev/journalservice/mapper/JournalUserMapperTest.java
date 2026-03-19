package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.response.JournalGroupResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.JournalUser;
import com.aquadev.journalservice.repository.JournalGroupRepository;
import com.aquadev.journalservice.service.group.JournalGroupUpsertService;
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

    @Mock
    JournalGroupUpsertService journalGroupUpsertService;

    @InjectMocks
    JournalUserMapper mapper;

    @Test
    void toEntity_success_mapsAllFields() {
        JournalGroup group = groupEntity(10L, "Group 10");
        given(journalGroupRepository.findByJournalGroupId(10L)).willReturn(Optional.of(group));

        JournalUserResponse source = buildResponse(10L, "Group 10", 1L, 2, "Stream", "Name");

        JournalUser entity = mapper.toEntity(source, 1L);

        assertThat(entity.getJournalUserId()).isEqualTo(1L);
        assertThat(entity.getStreamId()).isEqualByComparingTo(2);
        assertThat(entity.getStreamName()).isEqualTo("Stream");
        assertThat(entity.getFullName()).isEqualTo("Name");
        assertThat(entity.getJournalGroups()).hasSize(1);
        assertThat(entity.getJournalGroups().iterator().next().getJournalGroupId()).isEqualTo(10L);
    }

    @Test
    void toEntity_callsEnsureExistsWithNormalizedName() {
        JournalGroup group = groupEntity(10L, "Group 10");
        given(journalGroupRepository.findByJournalGroupId(10L)).willReturn(Optional.of(group));

        mapper.toEntity(buildResponse(10L, "Group 10", 1L, 2, "Stream", "Name"), 1L);

        verify(journalGroupUpsertService).ensureExists(10L, "Group 10");
    }

    @Test
    void toEntity_groupAlreadyExistsInDb_reusesExistingManagedEntity() {
        JournalGroup existingGroup = groupEntity(10L, "Group 10");
        given(journalGroupRepository.findByJournalGroupId(10L)).willReturn(Optional.of(existingGroup));

        JournalUser entity = mapper.toEntity(buildResponse(10L, "Group 10", 1L, 2, "Stream", "Name"), 1L);

        assertThat(entity.getJournalGroups()).hasSize(1);
        assertThat(entity.getJournalGroups().iterator().next()).isSameAs(existingGroup);
        verify(journalGroupUpsertService).ensureExists(10L, "Group 10");
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

    // ── helpers ──────────────────────────────────────────────────────────────

    private static JournalGroup groupEntity(Long journalGroupId, String name) {
        JournalGroup g = new JournalGroup();
        g.setJournalGroupId(journalGroupId);
        g.setName(name);
        return g;
    }

    private static JournalUserResponse buildResponse(Long groupId, String groupName,
                                                     Long studentId, Integer streamId,
                                                     String streamName, String fullName) {
        return new JournalUserResponse(
                List.of(new JournalGroupResponse(1, true, groupId, groupName)),
                null, studentId, groupId, fullName, 0, streamId, streamName, 1, "Photo",
                List.of(), List.of(), null, 1, LocalDate.now(), (short) 20,
                Instant.now(), Instant.now(), true, "Form"
        );
    }
}
