package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;

import java.util.List;

public interface JournalService {

    List<JournalHomeworkResponse> getHomeworksForUser(Integer page, Integer status, Integer type);
}
