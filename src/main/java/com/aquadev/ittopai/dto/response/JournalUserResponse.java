package com.aquadev.ittopai.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record JournalUserResponse(
        List<JournalGroupResponse> groups,
        String manualLink,
        Long studentId,
        Long currentGroupId,
        String fullName,
        Integer achievesCount,
        Integer streamId,
        String streamName,
        Integer level,
        String photo,
        List<JournalGamePoint> gamingPoints,
        List<Object> spentGamingPoints,
        JournalVisibilityResponse visibility,
        Integer currentGroupStatus,
        LocalDate birthday,
        Short age,
        Instant lastDateVisit,
        Instant registrationDate,
        Boolean gender,
        String studyFormShortName
) {
}
