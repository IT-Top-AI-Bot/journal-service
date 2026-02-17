package com.aquadev.ittopai.model;

import com.aquadev.ittopai.dto.response.JournalHomeworkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "journal_homework")
public class JournalHomework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer idSpec;

    @Column(nullable = false)
    private Integer idTeach;

    @Column(nullable = false)
    private Integer idGroup;

    @Column(nullable = false)
    private String fioTeach;

    @Column(length = 1000, nullable = false)
    private String theme;

    @Column(nullable = false)
    private LocalDate completionTime;

    @Column(nullable = false)
    private LocalDate creationTime;

    @Column(nullable = false)
    private LocalDate overdueTime;

    private String filename;

    @Column(length = 1000)
    private String filePath;

    @Column(length = 2000)
    private String comment;

    @Column(length = 1000, nullable = false)
    private String nameSpec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JournalHomeworkStatus status;

    private Integer commonStatus;

    @Column(columnDefinition = "TEXT")
    private String homeworkStud;

    @Column(columnDefinition = "TEXT")
    private String homeworkComment;

    @Column(length = 1000)
    private String coverImage;
}
