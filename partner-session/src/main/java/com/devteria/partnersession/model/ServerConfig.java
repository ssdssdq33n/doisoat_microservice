package com.devteria.partnersession.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerConfig {

    private String sftpServerName;

    private String ip;

    private String port;

    private String username;

    private String password;

    private String downloadPath;

    private String savePath;

    private SalesPartner salesPartner;

    private SupplyPartner supplyPartner;
}
