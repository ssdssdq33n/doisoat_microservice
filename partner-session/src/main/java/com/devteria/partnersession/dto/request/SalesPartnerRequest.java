package com.devteria.partnersession.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesPartnerRequest {
    private String name;
    private String description;
    private String status;
    // config SalesPartner sftp server (sacombank)
    private String discountedProfits;

    private String sftpServerName;

    private String ip;

    private String port;

    private String username;

    private String password;

    private String downloadPath;

    private String savePath;
}
