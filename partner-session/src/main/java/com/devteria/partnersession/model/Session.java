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
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long SESSION_ID;

    @Column(name = "CONTROL_DATE")
    @Temporal(TemporalType.DATE)
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

    @ManyToMany
    @JoinTable(
            name = "SUPPLY_PARTNER_SESSION_DAY",
            joinColumns = @JoinColumn(name = "SESSION_ID", referencedColumnName = "SESSION_ID"),
            inverseJoinColumns = @JoinColumn(name = "SUPPLY_PARTNER_ID", referencedColumnName = "SUPPLY_PARTNER_ID"))
    private Set<SupplyPartner> supplyPartner = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "SALES_PARTNER_ID", referencedColumnName = "SALES_PARTNER_ID")
    private SalesPartner salesPartner;
}
