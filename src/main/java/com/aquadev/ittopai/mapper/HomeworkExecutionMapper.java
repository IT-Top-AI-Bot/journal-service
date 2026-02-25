package com.aquadev.ittopai.mapper;

import com.aquadev.ittopai.dto.request.HomeworkExecutionRequest;
import com.aquadev.ittopai.dto.response.HomeworkExecutionResponse;
import com.aquadev.ittopai.dto.response.HomeworkExecutionStatus;
import com.aquadev.ittopai.model.HomeworkExecution;
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
    HomeworkExecution toEntity(HomeworkExecutionRequest request);

    HomeworkExecutionResponse toResponse(HomeworkExecution entity);
}
