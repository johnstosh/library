package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate dateOfBirth;

    private LocalDate dateOfDeath;

    @Lob
    private String religiousAffiliation;

    @Lob
    private String birthCountry;

    @Lob
    private String nationality;

    @Lob
    private String briefBiography;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Photo> photos;
}
