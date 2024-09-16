package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProtonReport {
    private Long stt;
    private String serviceCode;
    private String timeCreated;
    private String supplyPartner;
    private Long amount;
    private String sl;
    private String traceId;
    private String systemTraceId;
    private String requestIdToPartner;
    private String status;
    private String ReasonsNotMatch;
}
