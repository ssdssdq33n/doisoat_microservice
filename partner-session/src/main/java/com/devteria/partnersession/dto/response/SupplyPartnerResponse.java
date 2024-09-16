package com.devteria.partnersession.dto.response;

import java.util.Date;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupplyPartnerResponse {
    private Long SUPPLY_PARTNER_ID;
    private String name;
    private String description;
    private Date CreatedDate;
    private Date UpdatedDate;
    private String created;
    private String updated;
    private String status;
    private String discountedProfits;

    private String sftpServerName;

    private String ip;

    private String port;

    private String username;

    private String password;

    private String downloadPath;

    private String savePath;
}
