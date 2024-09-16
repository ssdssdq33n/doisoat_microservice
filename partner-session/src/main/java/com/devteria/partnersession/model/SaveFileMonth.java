package com.devteria.partnersession.model;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "SaveFileMonth")
public class SaveFileMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long FILE_ID;

    @Column(name = "NAME_SESSION")
    String nameSession;

    @Column(name = "NAME_FILE")
    String nameFile;

    public SaveFileMonth(String nameSession, String nameFile) {
        this.nameSession = nameSession;
        this.nameFile = nameFile;
    }
}
