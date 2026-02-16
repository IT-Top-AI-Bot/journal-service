package com.aquadev.ittopai.model;

import jakarta.persistence.*;
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

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
