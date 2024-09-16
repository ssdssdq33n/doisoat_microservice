package com.devteria.partnersession.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PathFileByDayRequest {
    private String irisReportFilePath;
    private String protonReportFilePath;
    private String stbReportFilePath;
    private int year;
    private int month;
}
