package com.aquadev.journalservice.mapper;

import com.aquadev.journalservice.dto.request.HomeworkExecutionRequest;
import com.aquadev.journalservice.dto.response.HomeworkExecutionResponse;
import com.aquadev.journalservice.dto.response.HomeworkExecutionStatus;
import com.aquadev.journalservice.model.HomeworkExecution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, imports = HomeworkExecutionStatus.class)
public interface HomeworkExecutionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(HomeworkExecutionStatus.PENDING)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "resultS3Key", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    HomeworkExecution toEntity(HomeworkExecutionRequest request);

    HomeworkExecutionResponse toResponse(HomeworkExecution entity);
}
