package com.aquadev.journalservice.controller.telegram.user;

import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.UserResponse;
import com.aquadev.journalservice.mapper.UserMapper;
import com.aquadev.journalservice.model.User;
import com.aquadev.journalservice.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram/users")
public class UserControllerImpl implements UserController {

    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getMe() {
        User user = userService.getMe();
        return userMapper.toResponse(user);
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return userMapper.toResponse(user);
    }

    @Override
    @PutMapping("/me/credentials")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateCredentials(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.updateCredentials(request);
        return userMapper.toResponse(user);
    }
}
