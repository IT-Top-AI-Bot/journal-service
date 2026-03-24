package com.aquadev.journalservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "journal_user")
public class JournalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<JournalGroup> journalGroups;

    @Column(nullable = false)
    private Long journalUserId;

    @Column(nullable = false)
    private Integer streamId;

    @Column(nullable = false)
    private String streamName;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 2048)
    private String photoUrl;

    private LocalDate birthday;

    private Instant lastDateVisit;

    private Instant registrationDate;

    @Column(nullable = false)
    private Boolean gender;
}
