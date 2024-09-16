package com.devteria.partnersession.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionRequest {
    private String name;
    private String dateControlRequest;
    private List<Long> supplyPartnerId;
    private Long salesPartnerId;
}
