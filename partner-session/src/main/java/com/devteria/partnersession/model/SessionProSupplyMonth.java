package com.devteria.partnersession.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
@Table(name = "session_pro_iris_month")
public class SessionProSupplyMonth {
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

    @ManyToMany
    @JoinTable(
            name = "SUPPLY_PARTNER_SESSION_MONTH",
            joinColumns = @JoinColumn(name = "SESSION_ID", referencedColumnName = "SESSION_ID"),
            inverseJoinColumns = @JoinColumn(name = "SUPPLY_PARTNER_ID", referencedColumnName = "SUPPLY_PARTNER_ID"))
    private Set<SupplyPartner> supplyPartner = new HashSet<>();
}
