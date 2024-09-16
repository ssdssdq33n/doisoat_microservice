package com.devteria.partnersession.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgumentControlByDayRequest {
    private String name; // "DS030724019",
    private String salesPartner; // "Sacombank",
    private String supplyPartner; // "Iris",
    private String dateControl; // "03/07/2024",

    private String validFromDate;
    private String validToDate;
}
