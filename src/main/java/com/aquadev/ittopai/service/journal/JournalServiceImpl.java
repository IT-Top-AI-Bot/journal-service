package com.aquadev.ittopai.service.journal;

import com.aquadev.ittopai.dto.response.JournalCountHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalHomeworkCounterType;
import com.aquadev.ittopai.dto.response.JournalScheduleResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final RestClient restClient;

    @Override
    public JournalUserResponse getCurrentUser() {
        return restClient.get()
                .uri("/settings/user-info")
                .retrieve()
                .body(JournalUserResponse.class);
    }

    @Override
    public List<JournalCountHomeworkResponse> getCountHomework() {
        List<JournalCountHomeworkResponse> response = restClient.get()
                .uri("/count/homework")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (response == null) {
            return List.of();
        }

        return response.stream()
                .filter(item -> item.counterTypeName() != JournalHomeworkCounterType.IGNORED)
                .toList();
    }

    @Override
    public List<JournalScheduleResponse> getScheduleByDate(LocalDate date) {
        return restClient.get()
                .uri(uri -> uri
                        .path("/schedule/operations/get-month")
                        .queryParam("date-filter", date)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
