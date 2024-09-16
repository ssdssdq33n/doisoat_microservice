package com.devteria.partnersession.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.SalesPartner;

@Repository
public interface SalesPartnerRepository extends JpaRepository<SalesPartner, Long> {
    Optional<SalesPartner> findSalesPartnerByNameEquals(String name);

    List<SalesPartner> findAllByStatusNotContaining(String status);

    List<SalesPartner> findAllByStatusEquals(String status);
}
