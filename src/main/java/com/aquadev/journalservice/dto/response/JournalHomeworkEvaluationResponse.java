package com.aquadev.journalservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record JournalHomeworkEvaluationResponse(
        Long id,
        @JsonProperty("id_dom_zad")
        Long homeworkId,
        @JsonProperty("id_stud")
        Long studentId,
        Integer mark,
        String comment,
        Set<String> tags
) {
}
