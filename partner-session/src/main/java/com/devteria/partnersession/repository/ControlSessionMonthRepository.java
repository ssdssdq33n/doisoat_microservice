package com.devteria.partnersession.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.ControlSessionMonth;

@Repository
public interface ControlSessionMonthRepository extends JpaRepository<ControlSessionMonth, Long> {
    ControlSessionMonth findBySessionName(String sessionName);

    Optional<ControlSessionMonth> findBySessionNameContaining(String name);
}
