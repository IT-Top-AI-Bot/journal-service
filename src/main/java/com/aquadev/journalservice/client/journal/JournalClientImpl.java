package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkEvaluationResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkStatus;
import com.aquadev.journalservice.dto.response.JournalHomeworkUploadResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@RateLimiter(name = "journalApi")
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
                .filter(item -> item.counterTypeName() != JournalHomeworkStatus.NOT_COMPLETED)
                .toList();
    }

    @Override
    public List<JournalHomeworkResponse> getHomeworks(Integer page, Integer status, Integer type, Integer groupId, Integer specId) {
        return restClient.get()
                .uri(uri -> uri
                        .path("/homework/operations/list")
                        .queryParam("page", page)
                        .queryParam("status", status)
                        .queryParam("type", type)
                        .queryParam("group_id", groupId)
                        .queryParamIfPresent("spec_id", Optional.ofNullable(specId))
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public Optional<JournalHomeworkEvaluationResponse> getHomeworkEvaluation(Long homeworkId) {
        try {
            JournalHomeworkEvaluationResponse response = restClient.get()
                    .uri(uri -> uri
                            .path("/homework/evaluation/operations/get")
                            .queryParam("id", homeworkId)
                            .build())
                    .retrieve()
                    .body(JournalHomeworkEvaluationResponse.class);

            return Optional.ofNullable(response);
        } catch (HttpClientErrorException.NotFound _) {
            return Optional.empty();
        }
    }

    @Override
    public JournalHomeworkUploadResponse uploadHomework(Long homeworkId, InputStream file, long fileSize, String filename) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("id", homeworkId);
        builder.part("file", new InputStreamResource(file) {
            @Override
            public long contentLength() {
                return fileSize;
            }

            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.APPLICATION_OCTET_STREAM);

        MultiValueMap<String, HttpEntity<?>> parts = builder.build();

        return restClient.post()
                .uri("/homework/operations/create")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(JournalHomeworkUploadResponse.class);
    }

    @Override
    public JournalHomeworkUploadResponse uploadHomeworkText(Long homeworkId, String text) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("id", homeworkId);
        builder.part("answerText", text);
        builder.part("spentTimeHour", "00");
        builder.part("spentTimeMinute", "01");

        return restClient.post()
                .uri("/homework/operations/create")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(JournalHomeworkUploadResponse.class);
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

    @Override
    public List<JournalSpecResponse> getGroupSpecs() {
        return restClient.get()
                .uri("/settings/group-specs")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
