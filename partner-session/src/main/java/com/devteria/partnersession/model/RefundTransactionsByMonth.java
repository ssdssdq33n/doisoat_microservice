package com.devteria.partnersession.model;

import java.util.Date;

import jakarta.persistence.*;

import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "ListOfRefundTransactionsByMonth")
@Transactional
public class RefundTransactionsByMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundTransactionsByMonth_ID;

    @Column(name = "TransactionNumber_ID")
    private String transactionNumber_ID;

    @Column(name = "Day_Trading")
    private Date dayTrading;

    @Column(name = "Educational_Time")
    private Date educationalTime;

    @Column(name = "Amount")
    private Long amount;

    @Column(name = "custom_Account_Code")
    private String customAccountCode;

    @Column(name = "Recorded_Account_Number")
    private String recordedAccountNumber;

    @Column(name = "Explanation")
    private String explanation;

    @Column(name = "TRACENO")
    private String traceNo;

    @Column(name = "SERVICE_PROVIDER")
    private String serviceProvider;

    @Column(name = "PAYMENT_SERVICE")
    private String paymentService;

    @Column(name = "Status")
    private String status;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "controlSessionMonth_ID", nullable = false)
    private ControlSessionMonth controlSessionMonth;
}
