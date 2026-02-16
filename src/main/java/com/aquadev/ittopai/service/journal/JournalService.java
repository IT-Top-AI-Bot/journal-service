package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.dto.response.JournalCountHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalScheduleResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;

import java.time.LocalDate;
import java.util.List;

public interface JournalService {

    JournalUserResponse getCurrentUser();

    List<JournalCountHomeworkResponse> getCountHomework();

    List<JournalScheduleResponse> getScheduleByDate(LocalDate month);
}
