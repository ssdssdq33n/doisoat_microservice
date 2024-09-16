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
@Table(name = "ControlSessionMonth")
public class ControlSessionMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long controlSessionMonth_ID;

    //    @OneToOne(mappedBy = "controlSessionMonth", cascade = CascadeType.ALL)
    //    private Session session;

    @Column(name = "session_name")
    private String sessionName;

    @JsonManagedReference
    @OneToMany(mappedBy = "controlSessionMonth", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefundTransactionsByMonth> refundTransactionsByMonths;
}
