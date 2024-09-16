package com.devteria.partnersession.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.SaveFileMonth;

@Repository
public interface SaveFileMonthRepository extends JpaRepository<SaveFileMonth, Long> {}
