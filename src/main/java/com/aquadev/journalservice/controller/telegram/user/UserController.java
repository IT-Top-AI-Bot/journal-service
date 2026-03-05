package com.aquadev.journalservice.controller.telegram.user;

import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telegram/users")
public interface UserController {

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    UserResponse getMe();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createUser(@Valid @RequestBody CreateUserRequest request);
}
