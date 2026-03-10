package com.aquadev.journalservice.controller.telegram.journal;

import com.aquadev.journalservice.client.journal.JournalClient;
import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.HomeworkExecutionResponse;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.mapper.HomeworkExecutionMapper;
import com.aquadev.journalservice.service.journal.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram/journal")
public class JournalControllerImpl implements JournalController {

    private final JournalClient journalClient;
    private final JournalService journalService;
    private final HomeworkExecutionMapper homeworkExecutionMapper;

    @Override
    @GetMapping("/me")
    public JournalUserResponse getCurrentUser() {
        return journalClient.getCurrentUser();
    }

    @Override
    @GetMapping("/homework/count")
    public List<JournalCountHomeworkResponse> getCountHomework() {
        return journalClient.getCountHomework();
    }

    @Override
    @GetMapping("/homework")
    public List<JournalHomeworkResponse> getHomeworks(@RequestParam Integer page,
                                                      @RequestParam Integer status,
                                                      @RequestParam Integer type) {
        return journalService.getHomeworksForUser(page, status, type);
    }

    @Override
    @PostMapping("/homework/execute")
    @ResponseStatus(HttpStatus.CREATED)
    public HomeworkExecutionResponse executeHomework(@Valid @RequestBody HomeworkExecutionRequest request) {
        return homeworkExecutionMapper.toResponse(journalService.executeHomework(request));
    }

    @Override
    @GetMapping("/schedule/{date}")
    public List<JournalScheduleResponse> getScheduleByDate(@PathVariable LocalDate date) {
        return journalClient.getScheduleByDate(date);
    }
}
