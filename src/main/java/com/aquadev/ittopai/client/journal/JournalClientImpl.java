package com.aquadev.ittopai.client.journal;

import com.aquadev.ittopai.dto.response.JournalCountHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalHomeworkResponse;
import com.aquadev.ittopai.dto.response.JournalHomeworkStatus;
import com.aquadev.ittopai.dto.response.JournalScheduleResponse;
import com.aquadev.ittopai.dto.response.JournalUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JournalClientImpl implements JournalClient {

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
                .filter(item -> item.counterTypeName() != JournalHomeworkStatus.IGNORED)
                .toList();
    }

    @Override
    public List<JournalHomeworkResponse> getHomeworks(Integer page, Integer status, Integer type, Integer groupId) {
        return restClient.get()
                .uri(uri -> uri
                        .path("/homework/operations/list")
                        .queryParam("page", page)
                        .queryParam("status", status)
                        .queryParam("type", type)
                        .queryParam("group_id", groupId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
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
