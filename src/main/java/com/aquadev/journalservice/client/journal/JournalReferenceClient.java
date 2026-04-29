package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.time.LocalDate;
import java.util.List;

@HttpExchange
public interface JournalReferenceClient {

    @GetExchange("/schedule/operations/get-month")
    List<JournalScheduleResponse> getScheduleByDate(@RequestParam("date-filter") LocalDate date);

    @GetExchange("/schedule/operations/get-by-date-range")
    List<JournalScheduleResponse> getScheduleByDateRange(
            @RequestParam("date_start") LocalDate dateStart,
            @RequestParam("date_end") LocalDate dateEnd);

    @GetExchange("/settings/group-specs")
    List<JournalSpecResponse> getGroupSpecs();
}
