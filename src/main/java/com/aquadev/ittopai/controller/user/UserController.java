package com.aquadev.ittopai.controller.user;

import com.aquadev.ittopai.dto.request.CreateUserRequest;
import com.aquadev.ittopai.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;


public interface UserController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createUser(@Valid @RequestBody CreateUserRequest request);
}
