package com.devteria.partnersession.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.SupplyPartner;

@Repository
public interface SupplyPartnerRepository extends JpaRepository<SupplyPartner, Long> {
    Optional<SupplyPartner> findSupplyPartnerByNameEquals(String name);

    List<SupplyPartner> findAllByStatusNotContaining(String status);

    List<SupplyPartner> findAllByStatusEquals(String status);
}
