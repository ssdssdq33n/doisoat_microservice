package com.devteria.partnersession.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "salesPartner")
public class SalesPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long SALES_PARTNER_ID;

    @Column(name = "NAME", length = 100)
    private String name;

    @Column(name = "Description")
    private String description;

    @Column(name = "CREATED_DATE")
    private Date CreatedDate;

    @Column(name = "UPDATED_DATE")
    private Date UpdatedDate;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "discounted_profits")
    private String discountedProfits;

    @Column(name = "sftp_server_name", nullable = false)
    private String sftpServerName;

    @Column(name = "ip_address", nullable = false)
    private String ip;

    @Column(name = "port", nullable = false)
    private String port;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "download_path", nullable = false)
    private String downloadPath;

    @Column(name = "save_path", nullable = false)
    private String savePath;

    @OneToMany(mappedBy = "salesPartner", cascade = CascadeType.ALL)
    private List<Session> sessions;
}
