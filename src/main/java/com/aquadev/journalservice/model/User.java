package com.aquadev.journalservice.model;

import com.aquadev.journalservice.generator.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @Column(nullable = false, unique = true)
    private Long telegramId;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JournalUser journalUser;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JournalCredential journalCredential;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
