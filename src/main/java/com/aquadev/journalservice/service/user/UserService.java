package com.aquadev.journalservice.service.user;

import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.model.User;

public interface UserService {

    User getMe();

    User getUserByTelegramId(Long telegramId);

    User createUser(CreateUserRequest request);
}
