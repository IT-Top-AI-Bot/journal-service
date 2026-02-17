package com.aquadev.ittopai.client.journal;

import com.aquadev.ittopai.dto.response.JournalCountHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalScheduleResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;

import java.time.LocalDate;
import java.util.List;

public interface JournalClient {

    JournalUserResponse getCurrentUser();

    List<JournalCountHomeworkResponse> getCountHomework();

    List<JournalHomeworkResponse> getHomeworks(Integer page, Integer status, Integer type, Integer groupId);

    List<JournalScheduleResponse> getScheduleByDate(LocalDate month);
}
