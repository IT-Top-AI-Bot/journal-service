package com.aquadev.ittopai.mapper;

import com.aquadev.ittopai.dto.request.CreateUserRequest;
import com.aquadev.ittopai.dto.response.UserResponse;
import com.aquadev.ittopai.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "journalCredential.username", source = "journalUsername")
    @Mapping(target = "journalCredential.password", source = "journalPassword")
    User toEntity(CreateUserRequest request);

    @Mapping(target = "journalUsername", source = "journalCredential.username")
    UserResponse toResponse(User user);
}
