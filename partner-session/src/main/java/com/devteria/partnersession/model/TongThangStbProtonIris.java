package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TongThangStbProtonIris {
    public Long stt;
    public String timeCreated;
    public Long amount;
    private String customAccountCode;
    private String salesPartner;
    private String supplyPartner;
    private String requestIdToPartner;
    private String systemTraceId;
    private String status;
    private String Reasons;
}
