package com.devteria.partnersession.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.SessionSalesProSupplyMonth;

@Repository
public interface SessionSalesProSupplyMonthRepository extends JpaRepository<SessionSalesProSupplyMonth, Long> {
    @Query(
            "SELECT s FROM SessionSalesProSupplyMonth s WHERE s.dateControl BETWEEN :startDate AND :endDate AND s.status = 'Thành công' "
                    + "AND s.dateCreated = (SELECT MAX(s2.dateCreated) FROM SessionSalesProSupplyMonth s2 WHERE s2.dateControl = s.dateControl AND s2.status = 'Thành công')")
    List<SessionSalesProSupplyMonth> findLatestSessionsByControlDate(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    Optional<SessionSalesProSupplyMonth> findSessionSalesProSupplyMonthByNameContaining(String name);

    List<SessionSalesProSupplyMonth> findAllByStatusNot(String status);

    Optional<SessionSalesProSupplyMonth> findSessionSalesProSupplyMonthByStatusEquals(String status);

    List<SessionSalesProSupplyMonth> findAllByNameContains(String id);

    Optional<SessionSalesProSupplyMonth> findTopByStatusEquals(String status);

    @Query("SELECT t FROM SessionSalesProSupplyMonth t WHERE t.status IN ('Đang xử lý', 'Chờ xử lý')")
    List<SessionSalesProSupplyMonth> findTransactionsWithSuccessOrFailureStatus();
}
