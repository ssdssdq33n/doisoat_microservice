package com.devteria.partnersession.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgumentControlByMonthRequest {
    private String name; // ten phien
    private String salesPartner; // doi tac
    private String dateControl; // thang
    private String supplyPartner; // iris

    private String validFromDate;
    private String validToDate;

    private int month;
    private int year;
}
