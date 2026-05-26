package com.aquadev.journalservice.service.journal;

import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.*;
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

    List<JournalScheduleResponse> getScheduleByDateRange(LocalDate dateStart, LocalDate dateEnd);

    List<JournalSpecResponse> getGroupSpecs();

    List<FutureExamResponse> getFutureExams();
}
