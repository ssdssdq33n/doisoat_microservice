package com.devteria.partnersession.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.ControlSessionMonth;
import com.devteria.partnersession.model.RefundTransactionsByMonth;

@Repository
public interface RefundTransactionsByMonthRepository extends JpaRepository<RefundTransactionsByMonth, Long> {
    List<RefundTransactionsByMonth> findAllByControlSessionMonth(ControlSessionMonth controlSessionMonth);
}
