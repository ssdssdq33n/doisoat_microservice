package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IrisReport {
    private String timeCreated;
    private String traceCode;
    private String topupStatus;
    private Long amount;
}
