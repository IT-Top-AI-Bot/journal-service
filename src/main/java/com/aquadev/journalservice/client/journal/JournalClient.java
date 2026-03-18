package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkEvaluationResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkUploadResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalClient {

    JournalUserResponse getCurrentUser();

    List<JournalCountHomeworkResponse> getCountHomework();

    List<JournalHomeworkResponse> getHomeworks(Integer page, Integer status, Integer type, Integer groupId, Integer specId);

    Optional<JournalHomeworkEvaluationResponse> getHomeworkEvaluation(Long homeworkId);

    JournalHomeworkUploadResponse uploadHomework(Long homeworkId, InputStream file, long fileSize);

    JournalHomeworkUploadResponse uploadHomeworkText(Long homeworkId, String text);

    List<JournalScheduleResponse> getScheduleByDate(LocalDate month);

    List<JournalSpecResponse> getGroupSpecs();
}
