package com.aquadev.journalservice.controller.telegram.journal;

import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/schedule/range")
    List<JournalScheduleResponse> getScheduleByDateRange(@RequestParam LocalDate dateStart,
                                                         @RequestParam LocalDate dateEnd);

    @GetMapping("/group-specs")
    List<JournalSpecResponse> getGroupSpecs();

    @GetMapping("/future-exams")
    List<FutureExamResponse> getFutureExams();
}
