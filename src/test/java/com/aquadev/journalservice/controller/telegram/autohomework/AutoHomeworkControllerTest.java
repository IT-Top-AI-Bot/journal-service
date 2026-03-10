package com.aquadev.journalservice.controller.telegram.autohomework;

import com.aquadev.journalservice.config.telegram.TelegramUserIdFilter;
import com.aquadev.journalservice.dto.request.UpdateAutoHomeworkSettingsRequest;
import com.aquadev.journalservice.dto.response.AutoHomeworkSettingsResponse;
import com.aquadev.journalservice.service.autohomework.AutoHomeworkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AutoHomeworkControllerTest {

    @Mock
    AutoHomeworkService autoHomeworkService;

    @InjectMocks
    AutoHomeworkControllerImpl controller;

    MockMvc mockMvc;

    final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HEADER = "X-Telegram-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .addFilters(new TelegramUserIdFilter())
                .build();
    }

    @Test
    void getSettings_returnsSettings_whenValidHeader() throws Exception {
        AutoHomeworkSettingsResponse settings = new AutoHomeworkSettingsResponse(true, null, Set.of(1L, 2L));
        when(autoHomeworkService.getSettings(12345L)).thenReturn(settings);

        mockMvc.perform(get("/api/v1/telegram/auto-homework/settings")
                        .header(HEADER, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getSettings_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/auto-homework/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSettings_returns400_whenInvalidHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/auto-homework/settings")
                        .header(HEADER, "bad-value"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSettings_returnsSettings_whenValidRequest() throws Exception {
        UpdateAutoHomeworkSettingsRequest request = new UpdateAutoHomeworkSettingsRequest(false, Set.of(3L));
        AutoHomeworkSettingsResponse settings = new AutoHomeworkSettingsResponse(false, null, Set.of(3L));
        when(autoHomeworkService.updateSettings(eq(12345L), any())).thenReturn(settings);

        mockMvc.perform(put("/api/v1/telegram/auto-homework/settings")
                        .header(HEADER, "12345")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updateSettings_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(put("/api/v1/telegram/auto-homework/settings")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
