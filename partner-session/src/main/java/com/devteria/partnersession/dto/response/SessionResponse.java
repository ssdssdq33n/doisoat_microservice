package com.devteria.partnersession.dto.response;

import java.util.Date;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionResponse {
    private Long SESSION_ID;
    private String name;
    private Date dateControl;
    private Date dateCreated;
    private Date dateUpdated;
    private String control;
    private String created;
    private String updated;
    private String message;
    private String status;
    private List<String> name_supplyPartner;
    private String name_salesPartner;
}
