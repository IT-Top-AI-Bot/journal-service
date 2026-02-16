package com.aquadev.ittopai.controller;

import com.aquadev.ittopai.dto.response.JournalCountHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalScheduleResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;
import com.aquadev.ittopai.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/journal")
public class JournalController {

    private final JournalService journalService;

    @GetMapping("/me")
    public JournalUserResponse getCurrentUser() {
        return journalService.getCurrentUser();
    }

    @GetMapping("/homework/count")
    public List<JournalCountHomeworkResponse> getCountHomework() {
        return journalService.getCountHomework();
    }

    @GetMapping("/schedule/{date}")
    public List<JournalScheduleResponse> getScheduleByDate(@PathVariable LocalDate date) {
        return journalService.getScheduleByDate(date);
    }

}
