package com.devteria.partnersession.model;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "ListOfRefundTransactionsByDay")
public class RefundTransactionsByDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refundTransactionsByDay_ID;
    // Số giao dịch ID
    @Column(name = "TransactionNumber_ID")
    private String transactionNumber_ID;
    // Ngày giao dịch
    @Column(name = "CreateTime")
    private String createTime;
    // Ngày hiệu lực
    @Column(name = "EffectiveDate")
    private String effectiveDate;
    // Số REF
    @Column(name = "SystemTraceId")
    private String systemTraceId;
    // Diễn giải
    @Column(name = "Explanation")
    private String explanation;
    // Số tiền giao dịch
    @Column(name = "Amount")
    private Long amount;
    // Số tài khoản ghi có
    @Column(name = "RecordedAccountNumber")
    private String recordedAccountNumber;
    // Số tài khoản ghi nợ
    @Column(name = "DebitAccountNumber")
    private String debitAccountNumber;
    // Mã tài khoản KH
    @Column(name = "CustomAccountCode")
    private String customAccountCode;
    // Kênh giao dịch
    @Column(name = "TransactionChannel")
    private String transactionChannel;
    // Trạng thái
    @Column(name = "Status")
    private String status;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controlSessionDay_ID", nullable = false)
    private ControlSessionDay controlSessionDay;
}
