package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange
public interface JournalHomeworkQueryClient {

    @GetExchange("/count/homework")
    List<JournalCountHomeworkResponse> getCountHomework(
            @RequestParam(required = false) Integer type,
            @RequestParam(value = "group_id", required = false) Integer groupId
    );

    @GetExchange("/homework/operations/list")
    List<JournalHomeworkResponse> getHomeworks(
            @RequestParam Integer page,
            @RequestParam Integer status,
            @RequestParam Integer type,
            @RequestParam("group_id") Integer groupId,
            @RequestParam(value = "spec_id", required = false) Integer specId
    );

    @GetExchange("/homework/operations/list")
    List<JournalHomeworkResponse> getHomeworks(
            @RequestParam Integer page,
            @RequestParam Integer status,
            @RequestParam Integer type,
            @RequestParam("group_id") Integer groupId,
            @RequestParam("spec_id[]") List<Integer> specIds
    );
}
