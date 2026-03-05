package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.response.JournalGroupResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.model.JournalGroup;
import com.aquadev.journalservice.model.JournalUser;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JournalUserMapper {

    private static final String FALLBACK_GROUP_PREFIX = "Group ";

    public JournalUser toEntity(JournalUserResponse source, long fallbackJournalUserId) {
        if (source == null) {
            throw new IllegalStateException("Journal user response is null");
        }

        JournalUser journalUser = new JournalUser();
        journalUser.setJournalUserId(source.studentId() != null ? source.studentId() : fallbackJournalUserId);
        journalUser.setStreamId(requireField(source.streamId(), "streamId"));
        journalUser.setStreamName(requireField(source.streamName(), "streamName"));
        journalUser.setFullName(requireField(source.fullName(), "fullName"));
        journalUser.setPhotoUrl(source.photo());
        journalUser.setBirthday(source.birthday());
        journalUser.setLastDateVisit(source.lastDateVisit());
        journalUser.setRegistrationDate(source.registrationDate());
        journalUser.setGender(requireField(source.gender(), "gender"));
        journalUser.setJournalGroups(mapGroups(source.groups(), source.currentGroupId()));

        return journalUser;
    }

    private Set<JournalGroup> mapGroups(List<JournalGroupResponse> sourceGroups, Long currentGroupId) {
        Map<Long, JournalGroup> uniqueGroups = new LinkedHashMap<>();

        if (sourceGroups != null) {
            for (JournalGroupResponse group : sourceGroups) {
                if (group == null || group.id() == null) {
                    continue;
                }
                uniqueGroups.put(group.id(), createGroup(group.id(), group.name()));
            }
        }

        if (currentGroupId != null) {
            uniqueGroups.computeIfAbsent(currentGroupId, id -> createGroup(id, null));
        }

        if (uniqueGroups.isEmpty()) {
            throw new IllegalStateException("Journal user does not contain any group information");
        }

        return new LinkedHashSet<>(uniqueGroups.values());
    }

    private JournalGroup createGroup(Long journalGroupId, String groupName) {
        JournalGroup group = new JournalGroup();
        group.setJournalGroupId(journalGroupId);
        group.setName(normalizeGroupName(journalGroupId, groupName));
        return group;
    }

    private String normalizeGroupName(Long journalGroupId, String groupName) {
        if (groupName != null && !groupName.isBlank()) {
            return groupName;
        }
        return FALLBACK_GROUP_PREFIX + journalGroupId;
    }

    private <T> T requireField(T value, String fieldName) {
        return Objects.requireNonNull(value, "Journal user field '" + fieldName + "' is required");
    }
}
