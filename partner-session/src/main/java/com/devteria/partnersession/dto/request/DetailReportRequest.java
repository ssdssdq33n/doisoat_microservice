package com.devteria.partnersession.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DetailReportRequest {
    private String validFromDate;
    private String validToDate;
    private String status;
    private String serviceProvider;
    private String service;
    private String username;
    private List<String> transType;
}
