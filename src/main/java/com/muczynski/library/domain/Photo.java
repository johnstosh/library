package com.muczynski.library.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    private String caption;

    private int rotation;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
}
