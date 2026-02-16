package com.aquadev.ittopai.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record JournalScheduleResponse(
        LocalDate date,
        Integer lesson,
        LocalTime startedAt,
        LocalTime finishedAt,
        String teacherName,
        String subjectName,
        String roomName
) {
}
