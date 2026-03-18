package com.aquadev.journalservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkExecutionRequest {
    private Long homeworkId;
    private Long specId;
    private Long teachId;
    private Long groupId;
    private String teacherFio;
    private String theme;
    private LocalDate completionTime;
    private LocalDate overdueTime;
    private String comment;
    private String nameSpec;
    private String homeworkUrl;

    @Size(max = 500)
    private String homeworkText;
}
