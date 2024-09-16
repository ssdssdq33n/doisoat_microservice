package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IrisMonthReport {
    private Long stt;
    private String timeCreated;
    private String partnerName;
    private String traceCode;
    private String phoneNumber;
    private Long amount;
    private String telco;
    private String topupStatus;
    private String topupStatusCode;
    private String reasonsNotMatch;
}
