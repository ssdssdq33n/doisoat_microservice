package com.devteria.partnersession.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResultDataSessionMonthSTBP {
    private String month;
    private Long total;
}
