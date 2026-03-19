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
     * Creates the group if it does not yet exist, or renames it if {@code updateName} is
     * provided and differs from the stored name.
     *
     * <p>{@code insertName} is used when creating a new row and may be a generated
     * fallback (e.g. "Group 42"). {@code updateName} must be {@code null} when the
     * canonical name is unknown — passing {@code null} prevents an existing group from
     * being overwritten with a generated placeholder.
     *
     * <p>Runs in a dedicated REQUIRES_NEW transaction so that a concurrent-insert race
     * (DataIntegrityViolationException on saveAndFlush) only rolls back this inner
     * transaction and never poisons the caller's outer transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(Long journalGroupId, String insertName, String updateName) {
        Optional<JournalGroup> existing = journalGroupRepository.findByJournalGroupId(journalGroupId);
        if (existing.isPresent()) {
            if (updateName != null) {
                JournalGroup group = existing.get();
                if (!updateName.equals(group.getName())) {
                    group.setName(updateName);
                    journalGroupRepository.save(group);
                }
            }
            return;
        }

        JournalGroup group = new JournalGroup();
        group.setJournalGroupId(journalGroupId);
        group.setName(insertName);
        try {
            journalGroupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException _) {
            // Another thread inserted the same group concurrently — it now exists in DB.
        }
    }
}
