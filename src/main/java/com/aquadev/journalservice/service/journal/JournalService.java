package com.aquadev.journalservice.service.journal;

import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.model.HomeworkExecution;

import java.util.List;

public interface JournalService {

    List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type);

    HomeworkExecution executeHomework(HomeworkExecutionRequest request);
}
