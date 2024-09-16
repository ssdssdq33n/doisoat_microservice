package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TongThangProIrisReport {
    private Long stt;
    private String timeCreated;
    private Long amount;
    private String phoneNumber;
    private String supplyPartner;
    private String irisTraceCode; // CC1647652
    private String stbSystemTraceId; // ATOMI24183251415

    private String status;
    private String ReasonsNotMatch;
}
