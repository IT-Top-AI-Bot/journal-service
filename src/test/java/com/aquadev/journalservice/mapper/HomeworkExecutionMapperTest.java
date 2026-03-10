package com.aquadev.journalservice.mapper;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.HomeworkExecutionResponse;
import com.aquadev.journalservice.model.HomeworkExecution;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HomeworkExecutionMapperTest {

    private final HomeworkExecutionMapper mapper = Mappers.getMapper(HomeworkExecutionMapper.class);

    @Test
    void toEntity_mapsFieldsCorrectly() {
        HomeworkExecutionRequest request = new HomeworkExecutionRequest(
                1L, 2L, 3L, 4L, "Teacher", "Theme", null, null, "Comment", "Spec", "http://url"
        );
        HomeworkExecution entity = mapper.toEntity(request);

        assertThat(entity.getHomeworkId()).isEqualTo(1L);
        assertThat(entity.getSpecId()).isEqualTo(2L);
        assertThat(entity.getStatus()).isEqualTo(HomeworkExecutionStatus.PENDING);
    }

    @Test
    void toResponse_mapsFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        HomeworkExecution entity = new HomeworkExecution();
        entity.setId(id);
        entity.setStatus(HomeworkExecutionStatus.DONE);

        HomeworkExecutionResponse response = mapper.toResponse(entity);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.status()).isEqualTo(HomeworkExecutionStatus.DONE);
    }
}
