package com.devteria.partnersession.model;

import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "ControlSessionDay")
public class ControlSessionDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long controlSessionDay_ID;

    @Column(name = "Session_name")
    private String sessionName;

    //    @OneToOne(mappedBy = "controlSessionDay", cascade = CascadeType.ALL)
    //    private Session session;

    @JsonManagedReference
    @OneToMany(mappedBy = "controlSessionDay", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefundTransactionsByDay> refundTransactionsByDays;
}
