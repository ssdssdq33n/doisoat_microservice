package com.devteria.partnersession.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.ControlSessionDay;

@Repository
public interface ControlSessionDayRepository extends JpaRepository<ControlSessionDay, Long> {
    Optional<ControlSessionDay> findBySessionNameEquals(String name);

    ControlSessionDay findBySessionName(String name);
}
