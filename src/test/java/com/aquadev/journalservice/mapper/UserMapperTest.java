package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.request.CreateUserRequest;
import com.aquadev.journalservice.dto.response.UserResponse;
import com.aquadev.journalservice.model.JournalCredential;
import com.aquadev.journalservice.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toEntity_mapsFieldsCorrectly() {
        CreateUserRequest request = new CreateUserRequest("juser", "jpass");
        User user = mapper.toEntity(request);

        assertThat(user.getJournalCredential().getUsername()).isEqualTo("juser");
        assertThat(user.getJournalCredential().getPassword()).isEqualTo("jpass");
    }

    @Test
    void toResponse_mapsFieldsCorrectly() {
        User user = new User();
        user.setJournalCredential(new JournalCredential());
        user.getJournalCredential().setUsername("juser");

        UserResponse response = mapper.toResponse(user);

        assertThat(response.journalUsername()).isEqualTo("juser");
    }
}
