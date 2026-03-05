package com.aquadev.journalservice.model;

import com.aquadev.journalservice.converter.PasswordCryptoConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JournalCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long journalUserId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @Convert(converter = PasswordCryptoConverter.class)
    private String password;
}
