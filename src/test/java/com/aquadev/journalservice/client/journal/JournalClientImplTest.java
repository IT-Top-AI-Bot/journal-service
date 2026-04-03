package com.aquadev.journalservice.client.journal;

import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkEvaluationResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkStatus;
import com.aquadev.journalservice.dto.response.JournalHomeworkUploadResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalSpecResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JournalClientImplTest {

    private JournalClientImpl journalClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        journalClient = new JournalClientImpl(builder.build());
    }

    @Test
    void getCurrentUser_success() throws Exception {
        JournalUserResponse expected = new JournalUserResponse(
                List.of(), null, 1L, 10L, "Name", 0, 2, "Stream", 1, "Photo",
                List.of(), List.of(), null, 1, LocalDate.now(), (short) 20,
                null, null, true, "Form"
        );

        mockServer.expect(requestTo("/settings/user-info"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        JournalUserResponse result = journalClient.getCurrentUser();

        assertThat(result.studentId()).isEqualTo(1L);
        assertThat(result.fullName()).isEqualTo("Name");
    }

    @Test
    void getCountHomework_success() {
        // Use JSON string to ensure we test deserialization correctly
        String json = """
                [
                    {"counter_type": 3, "counter": 5},
                    {"counter_type": 0, "counter": 3}
                ]
                """;

        mockServer.expect(requestTo("/count/homework"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<JournalCountHomeworkResponse> result = journalClient.getCountHomework();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().counterTypeName()).isEqualTo(JournalHomeworkStatus.EXPIRED);
        assertThat(result.getFirst().counter()).isEqualTo(3);
    }

    @Test
    void getHomeworks_withoutSpecId_doesNotAddSpecIdParam() throws Exception {
        List<JournalHomeworkResponse> expected = List.of();

        mockServer.expect(requestTo(containsString("/homework/operations/list")))
                .andExpect(queryParam("page", "1"))
                .andExpect(requestTo(not(containsString("spec_id"))))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        List<JournalHomeworkResponse> result = journalClient.getHomeworks(1, 1, 1, 10, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getHomeworks_withSpecId_includesSpecIdParam() throws Exception {
        List<JournalHomeworkResponse> expected = List.of();

        mockServer.expect(requestTo(containsString("/homework/operations/list")))
                .andExpect(queryParam("spec_id", "5"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        List<JournalHomeworkResponse> result = journalClient.getHomeworks(1, 1, 1, 10, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void getHomeworkEvaluation_success() throws Exception {
        JournalHomeworkEvaluationResponse expected = new JournalHomeworkEvaluationResponse(1L, 42L, 77L, 5, "Excellent", Set.of());

        mockServer.expect(requestTo(containsString("/homework/evaluation/operations/get")))
                .andExpect(queryParam("id", "42"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        Optional<JournalHomeworkEvaluationResponse> result = journalClient.getHomeworkEvaluation(42L);

        assertThat(result).isPresent();
        assertThat(result.get().mark()).isEqualTo(5);
    }

    @Test
    void getHomeworkEvaluation_notFound_returnsEmpty() {
        mockServer.expect(requestTo(containsString("/homework/evaluation/operations/get")))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        Optional<JournalHomeworkEvaluationResponse> result = journalClient.getHomeworkEvaluation(42L);

        assertThat(result).isEmpty();
    }

    @Test
    void uploadHomework_success() throws Exception {
        JournalHomeworkUploadResponse expected = new JournalHomeworkUploadResponse(1L, "file.pdf", "/path", "tmp", 0, LocalDate.now(), "answer", false);

        mockServer.expect(requestTo("/homework/operations/create"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        JournalHomeworkUploadResponse result = journalClient.uploadHomework(1L, new ByteArrayInputStream(new byte[0]), 0, "file.txt");

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void uploadHomeworkText_success() throws Exception {
        JournalHomeworkUploadResponse expected = new JournalHomeworkUploadResponse(2L, null, null, null, 0, LocalDate.now(), "text answer", false);

        mockServer.expect(requestTo("/homework/operations/create"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        JournalHomeworkUploadResponse result = journalClient.uploadHomeworkText(1L, "Мой ответ");

        assertThat(result.id()).isEqualTo(2L);
    }

    @Test
    void getGroupSpecs_success() throws Exception {
        List<JournalSpecResponse> expected = List.of(new JournalSpecResponse(10, "Math", "M"));

        mockServer.expect(requestTo("/settings/group-specs"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        List<JournalSpecResponse> result = journalClient.getGroupSpecs();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(10);
    }

    @Test
    void getScheduleByDate_success() throws Exception {
        List<JournalScheduleResponse> expected = List.of();
        LocalDate date = LocalDate.now();

        mockServer.expect(requestTo(containsString("/schedule/operations/get-month")))
                .andExpect(queryParam("date-filter", date.toString()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        List<JournalScheduleResponse> result = journalClient.getScheduleByDate(date);

        assertThat(result).isEmpty();
    }
}
