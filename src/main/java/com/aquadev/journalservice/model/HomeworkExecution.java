package com.aquadev.journalservice.model;

import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.journalservice.generator.GeneratedUuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "homework_execution")
public class HomeworkExecution {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    @Column(name = "spec_id", nullable = false)
    private Long specId;

    @Column(name = "teach_id", nullable = false)
    private Long teachId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "teacher_fio")
    private String teacherFio;

    @Column(name = "theme", length = 1024)
    private String theme;

    @Column(name = "completion_time")
    private LocalDate completionTime;

    @Column(name = "overdue_time")
    private LocalDate overdueTime;

    @Column(name = "comment", length = 1024)
    private String comment;

    @Column(name = "name_spec", length = 1024)
    private String nameSpec;

    @Column(name = "homework_url", length = 4096)
    private String homeworkUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HomeworkExecutionStatus status = HomeworkExecutionStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "result_s3_key", length = 4096)
    private String resultS3Key;

    @Column(name = "result_text", length = 4096)
    private String resultText;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
