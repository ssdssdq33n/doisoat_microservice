package com.devteria.partnersession.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.ControlSessionDay;
import com.devteria.partnersession.model.RefundTransactionsByDay;

@Repository
public interface RefundTransactionsByDayRepository extends JpaRepository<RefundTransactionsByDay, Long> {

    List<RefundTransactionsByDay> findAllByControlSessionDay(ControlSessionDay controlSessionDay);

    List<RefundTransactionsByDay> findAllByControlSessionDayAndStatusEquals(
            ControlSessionDay controlSessionDay, String status);
}
