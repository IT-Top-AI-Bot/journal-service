package com.aquadev.journalservice.controller.telegram.journal;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.config.telegram.TelegramUserIdFilter;
import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.HomeworkExecutionResponse;
import com.aquadev.journalservice.dto.response.JournalCountHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalHomeworkResponse;
import com.aquadev.journalservice.dto.response.JournalScheduleResponse;
import com.aquadev.journalservice.dto.response.JournalUserResponse;
import com.aquadev.journalservice.mapper.HomeworkExecutionMapper;
import com.aquadev.journalservice.model.HomeworkExecution;
import com.aquadev.journalservice.service.journal.JournalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JournalControllerTest {

    @Mock
    JournalService journalService;

    @Mock
    HomeworkExecutionMapper homeworkExecutionMapper;

    @InjectMocks
    JournalControllerImpl controller;

    MockMvc mockMvc;

    final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HEADER = "X-Telegram-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .addFilters(new TelegramUserIdFilter())
                .setConversionService(new DefaultFormattingConversionService())
                .build();
    }

    @Test
    void getCurrentUser_returnsOk_whenValidHeader() throws Exception {
        JournalUserResponse userResponse = new JournalUserResponse(
                null, null, 12345L, null, "John Doe",
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null);
        when(journalService.getCurrentUser()).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/telegram/journal/me")
                        .header(HEADER, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(12345))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    void getCurrentUser_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/journal/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCountHomework_returnsList_whenValidHeader() throws Exception {
        JournalCountHomeworkResponse countResp = new JournalCountHomeworkResponse(1, 5);
        when(journalService.getCountHomework()).thenReturn(List.of(countResp));

        mockMvc.perform(get("/api/v1/telegram/journal/homework/count")
                        .header(HEADER, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].counter").value(5));
    }

    @Test
    void getCountHomework_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/journal/homework/count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getHomeworks_returnsList_whenValidParams() throws Exception {
        JournalHomeworkResponse hw = new JournalHomeworkResponse(
                1, 10, 20, 30, "Teacher", "Theme",
                null, null, null, null, null, null,
                "Math", 0, 0, null, null, null);
        when(journalService.getHomeworksForUser(0, 0, 1)).thenReturn(List.of(hw));

        mockMvc.perform(get("/api/v1/telegram/journal/homework")
                        .header(HEADER, "12345")
                        .param("page", "0")
                        .param("status", "0")
                        .param("type", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nameSpec").value("Math"))
                .andExpect(jsonPath("$[0].theme").value("Theme"));
    }

    @Test
    void getHomeworks_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/journal/homework")
                        .param("page", "0")
                        .param("status", "0")
                        .param("type", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void executeHomework_returnsCreated_whenValidRequest() throws Exception {
        HomeworkExecutionRequest request = new HomeworkExecutionRequest(
                1L, 2L, 3L, 4L, "Teacher", "Theme",
                null, null, null, "Math", null);
        UUID execId = UUID.randomUUID();
        HomeworkExecutionResponse execResp = new HomeworkExecutionResponse(
                execId, 1L, 2L, 3L, 4L, "Teacher", "Theme",
                null, null, null, "Math",
                null, HomeworkExecutionStatus.PENDING, null, null, null);

        when(journalService.executeHomework(any())).thenReturn(mock(HomeworkExecution.class));
        when(homeworkExecutionMapper.toResponse(any())).thenReturn(execResp);

        mockMvc.perform(post("/api/v1/telegram/journal/homework/execute")
                        .header(HEADER, "12345")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(execId.toString()))
                .andExpect(jsonPath("$.nameSpec").value("Math"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void executeHomework_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/journal/homework/execute")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getScheduleByDate_returnsList_whenValidHeader() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 10);
        JournalScheduleResponse schedule = new JournalScheduleResponse(
                null, 1, null, null,
                "Teacher", "Math", "101");
        when(journalService.getScheduleByDate(date)).thenReturn(List.of(schedule));

        mockMvc.perform(get("/api/v1/telegram/journal/schedule/{date}", date)
                        .header(HEADER, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectName").value("Math"))
                .andExpect(jsonPath("$[0].lesson").value(1));
    }

    @Test
    void getScheduleByDate_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/journal/schedule/{date}", LocalDate.now()))
                .andExpect(status().isUnauthorized());
    }
}
