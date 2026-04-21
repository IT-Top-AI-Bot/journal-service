package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.UserResponse;
import com.aquadev.journalservice.model.User;
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
    @Mapping(target = "fullName",
            expression = "java(user.getJournalUser() != null ? user.getJournalUser().getFullName() : null)")
    @Mapping(target = "credentialsInvalid",
            expression = "java(user.getJournalUser() != null && user.getJournalUser().isCredentialsInvalid())")
    UserResponse toResponse(User user);
}
