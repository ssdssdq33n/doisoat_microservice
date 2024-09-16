package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StbReport {
    private Long stt;
    private String transactionNumberId;
    private String createTime;
    private String effectiveDate;
    private String systemTraceId;
    private String explain;
    private Long amount;
    private String recordedAccountNumber;
    private String debitAccountNumber;
    private String customAccountCode;
    private String transactionChannel;
    private String status;
}
