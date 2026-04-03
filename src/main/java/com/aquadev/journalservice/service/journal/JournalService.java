package com.aquadev.journalservice.service.journal;

import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.model.HomeworkExecution;

import java.time.LocalDate;
import java.util.List;

public interface JournalService {

    JournalUserResponse getCurrentUser();

    List<JournalCountHomeworkResponse> getCountHomework();

    Long getCurrentGroupId();

    List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type);

    HomeworkExecution executeHomework(HomeworkExecutionRequest request);

    List<JournalScheduleResponse> getScheduleByDate(LocalDate date);

    List<JournalSpecResponse> getGroupSpecs();
}
