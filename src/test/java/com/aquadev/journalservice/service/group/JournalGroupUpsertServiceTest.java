package com.aquadev.journalservice.service.group;

import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.repository.JournalGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JournalGroupUpsertServiceTest {

    @Mock
    JournalGroupRepository journalGroupRepository;

    JournalGroupUpsertService service;

    @BeforeEach
    void setUp() {
        service = new JournalGroupUpsertService(journalGroupRepository);
    }

    @Test
    void ensureExists_groupAbsent_savesNewGroup() {
        given(journalGroupRepository.findByJournalGroupId(42L)).willReturn(Optional.empty());

        service.ensureExists(42L, "Math");

        ArgumentCaptor<JournalGroup> captor = ArgumentCaptor.forClass(JournalGroup.class);
        verify(journalGroupRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getJournalGroupId()).isEqualTo(42L);
        assertThat(captor.getValue().getName()).isEqualTo("Math");
    }

    @Test
    void ensureExists_groupPresent_sameName_doesNotSave() {
        JournalGroup existing = groupEntity(42L, "Math");
        given(journalGroupRepository.findByJournalGroupId(42L)).willReturn(Optional.of(existing));

        service.ensureExists(42L, "Math");

        verify(journalGroupRepository, never()).save(any());
        verify(journalGroupRepository, never()).saveAndFlush(any());
    }

    @Test
    void ensureExists_groupPresent_staleName_updatesName() {
        JournalGroup existing = groupEntity(42L, "Old Name");
        given(journalGroupRepository.findByJournalGroupId(42L)).willReturn(Optional.of(existing));

        service.ensureExists(42L, "New Name");

        assertThat(existing.getName()).isEqualTo("New Name");
        verify(journalGroupRepository).save(existing);
    }

    @Test
    void ensureExists_concurrentInsert_swallowsConstraintViolation() {
        given(journalGroupRepository.findByJournalGroupId(42L)).willReturn(Optional.empty());
        willThrow(new DataIntegrityViolationException("duplicate key"))
                .given(journalGroupRepository).saveAndFlush(any());

        // Must not throw — the concurrent insert means the group already exists in DB
        service.ensureExists(42L, "Math");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static JournalGroup groupEntity(Long journalGroupId, String name) {
        JournalGroup g = new JournalGroup();
        g.setJournalGroupId(journalGroupId);
        g.setName(name);
        return g;
    }
}
