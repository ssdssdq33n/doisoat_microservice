package com.devteria.partnersession.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResultDataSessionMonth {
    private String month;
    private Long total;
    private Long total_fail;
    private Long total_notmacth;
    private Long total_success;
}
