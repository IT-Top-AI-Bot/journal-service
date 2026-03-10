package com.aquadev.journalservice.controller.telegram.user;

import com.aquadev.journalservice.config.telegram.TelegramUserIdFilter;
import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.UserResponse;
import com.aquadev.journalservice.mapper.UserMapper;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class UserControllerTest {

    @Mock
    UserService userService;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserControllerImpl controller;

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
    void getMe_returnsUser_whenValidHeader() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id, 12345L, "user1", null, null);
        when(userService.getMe()).thenReturn(mock(User.class));
        when(userMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/telegram/users/me")
                        .header(HEADER, "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramId").value(12345))
                .andExpect(jsonPath("$.journalUsername").value("user1"));
    }

    @Test
    void getMe_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_returns400_whenInvalidHeader() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/users/me")
                        .header(HEADER, "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMe_returns400_whenNegativeTelegramId() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/users/me")
                        .header(HEADER, "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_returnsCreated_whenValidRequest() throws Exception {
        UUID id = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest("username", "password");
        UserResponse response = new UserResponse(id, 12345L, "username", null, null);
        when(userService.createUser(any())).thenReturn(mock(User.class));
        when(userMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/telegram/users")
                        .header(HEADER, "12345")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telegramId").value(12345))
                .andExpect(jsonPath("$.journalUsername").value("username"));
    }

    @Test
    void createUser_returns401_whenMissingHeader() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/users")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
