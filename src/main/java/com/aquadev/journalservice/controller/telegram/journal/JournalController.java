package com.aquadev.journalservice.controller.telegram.journal;

import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.HomeworkExecutionResponse;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;
import java.util.List;


@RequestMapping("/api/v1/telegram/journal")
public interface JournalController {

    @GetMapping("/me")
    JournalUserResponse getCurrentUser();

    @GetMapping("/homework/count")
    List<JournalCountHomeworkResponse> getCountHomework();

    @GetMapping("/homework")
    List<JournalHomeworkResponse> getHomeworks(@RequestParam Integer page,
                                               @RequestParam Integer status,
                                               @RequestParam Integer type);

    @PostMapping("/homework/execute")
    @ResponseStatus(HttpStatus.CREATED)
    HomeworkExecutionResponse executeHomework(@Valid @RequestBody HomeworkExecutionRequest request);

    @GetMapping("/schedule/{date}")
    List<JournalScheduleResponse> getScheduleByDate(@PathVariable LocalDate date);

    @GetMapping("/group-specs")
    List<JournalSpecResponse> getGroupSpecs();
}
