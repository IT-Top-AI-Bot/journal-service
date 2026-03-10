package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.response.ErrorResponse;
import com.aquadev.journalservice.exception.base.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionMapperTest {

    private final ExceptionMapper mapper = Mappers.getMapper(ExceptionMapper.class);

    @Test
    void toErrorResponse_mapsFieldsCorrectly() {
        NotFoundException ex = new NotFoundException("Not Found") {
        };
        ErrorResponse response = mapper.toErrorResponse(ex, "/api/test");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.message()).isEqualTo("Not Found");
        assertThat(response.path()).isEqualTo("/api/test");
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void getStatus_returnsInternalServerError_whenNoAnnotation() {
        Exception ex = new RuntimeException("Error");
        assertThat(mapper.getStatus(ex)).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
