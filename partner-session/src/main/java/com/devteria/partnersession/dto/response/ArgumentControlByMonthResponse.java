package com.devteria.partnersession.dto.response;

import java.util.List;

import com.devteria.partnersession.model.TongThangReport;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArgumentControlByMonthResponse {
    private List<TongThangReport> tongThangReports;
}
