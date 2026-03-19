package com.aquadev.journalservice.service.group;

import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.repository.JournalGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JournalGroupUpsertService {

    private final JournalGroupRepository journalGroupRepository;

    /**
     * Creates the group if it does not yet exist, or updates its name if it has changed.
     * Runs in a dedicated REQUIRES_NEW transaction so that a concurrent-insert race
     * (DataIntegrityViolationException on saveAndFlush) only rolls back this inner
     * transaction and never poisons the caller's outer transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(Long journalGroupId, String name) {
        Optional<JournalGroup> existing = journalGroupRepository.findByJournalGroupId(journalGroupId);
        if (existing.isPresent()) {
            JournalGroup group = existing.get();
            if (!name.equals(group.getName())) {
                group.setName(name);
                journalGroupRepository.save(group);
            }
            return;
        }

        JournalGroup group = new JournalGroup();
        group.setJournalGroupId(journalGroupId);
        group.setName(name);
        try {
            journalGroupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException _) {
            // Another thread inserted the same group concurrently — it now exists in DB.
        }
    }
}
