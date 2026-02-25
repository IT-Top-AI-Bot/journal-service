package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.dto.request.HomeworkExecutionRequest;
import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;
import com.aquadev.ittopai.model.HomeworkExecution;

import java.util.List;

public interface JournalService {

    List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type);

    HomeworkExecution executeHomework(HomeworkExecutionRequest request);
}
