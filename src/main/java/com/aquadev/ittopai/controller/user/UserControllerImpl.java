package com.aquadev.ittopai.controller.user;

import com.aquadev.ittopai.dto.request.CreateUserRequest;
import com.aquadev.ittopai.dto.response.UserResponse;
import com.aquadev.ittopai.mapper.UserMapper;
import com.aquadev.ittopai.model.User;
import com.aquadev.ittopai.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserControllerImpl implements UserController {

    private final UserMapper userMapper;
    private final UserService userService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return userMapper.toResponse(user);
    }
}
