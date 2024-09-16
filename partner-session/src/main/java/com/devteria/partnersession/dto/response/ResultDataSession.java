package com.devteria.partnersession.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResultDataSession {
    private String day;
    private String dateControl;
    private Double hoantraAmount;
    private Double khongxacdinhAmount;
    private Double result;
}
