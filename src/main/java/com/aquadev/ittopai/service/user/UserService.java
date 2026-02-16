package com.aquadev.ittopai.service.user;

import com.aquadev.ittopai.dto.request.CreateUserRequest;
import com.aquadev.ittopai.model.User;

public interface UserService {

    User getUserByTelegramId(Long telegramId);

    User createUser(CreateUserRequest request);
}
