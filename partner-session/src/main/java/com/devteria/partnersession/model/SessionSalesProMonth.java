package com.devteria.partnersession.model;

import java.util.Date;

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
@Table(name = "session_stb_pro_month")
public class SessionSalesProMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long SESSION_ID;

    @Column(name = "CONTROL_DATE")
    private Date dateControl;

    @Column(name = "CREATED_DATE")
    private Date dateCreated;

    @Column(name = "UPDATED_DATE")
    private Date dateUpdated;

    @Column(name = "NAME", length = 100)
    private String name;

    @Column(name = "Description")
    private String description;

    @Column(name = "MESSAGE")
    private String message;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "total")
    private Long total;

    @Column(name = "total_success")
    private Long totalSuccess;

    @Column(name = "total_fail")
    private Long totalFail;

    @Column(name = "total_notmatch")
    private Long totalNotMatch;

    @ManyToOne
    @JoinColumn(name = "SALES_PARTNER_ID", referencedColumnName = "SALES_PARTNER_ID")
    private SalesPartner salesPartner;
}
