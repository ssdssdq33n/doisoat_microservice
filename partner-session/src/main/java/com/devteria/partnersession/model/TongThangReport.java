package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TongThangReport {
    private String transactionNumberId;
    private String timeCreated;
    private String educationalTime;
    private Long amount;
    private String customAccountCode;
    private String RecordedAccountNumber;
    private String explain;
    private String traceNo;
    private String serviceProvider;
    private String paymentService;
    private String status;

    private String SystemTraceId;

    private String TraceCode;
}
