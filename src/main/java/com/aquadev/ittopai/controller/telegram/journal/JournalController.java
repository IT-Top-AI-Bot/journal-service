package com.aquadev.ittopai.controller.telegram.journal;

import com.aquadev.ittopai.client.journal.JournalClient;
import com.aquadev.ittopai.dto.request.HomeworkExecutionRequest;
import com.aquadev.ittopai.dto.response.HomeworkExecutionResponse;
import com.aquadev.ittopai.dto.response.JournalCountHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalScheduleResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;
import com.aquadev.ittopai.mapper.HomeworkExecutionMapper;
import com.aquadev.ittopai.service.journal.JournalService;
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
public class JournalController {

    private final JournalClient journalClient;
    private final JournalService journalService;
    private final HomeworkExecutionMapper homeworkExecutionMapper;

    @GetMapping("/me")
    public JournalUserResponse getCurrentUser() {
        return journalClient.getCurrentUser();
    }

    @GetMapping("/homework/count")
    public List<JournalCountHomeworkResponse> getCountHomework() {
        return journalClient.getCountHomework();
    }

    @GetMapping("/homework")
    public List<JournalHomeworkResponse> getHomeworks(@RequestParam Integer page,
                                                      @RequestParam Integer status,
                                                      @RequestParam Integer type) {
        return journalService.getHomeworksForUser(page, status, type);
    }

    @PostMapping("/homework/execute")
    @ResponseStatus(HttpStatus.CREATED)
    public HomeworkExecutionResponse executeHomework(@Valid @RequestBody HomeworkExecutionRequest request) {
        return homeworkExecutionMapper.toResponse(journalService.executeHomework(request));
    }

    @GetMapping("/schedule/{date}")
    public List<JournalScheduleResponse> getScheduleByDate(@PathVariable LocalDate date) {
        return journalClient.getScheduleByDate(date);
    }

}
