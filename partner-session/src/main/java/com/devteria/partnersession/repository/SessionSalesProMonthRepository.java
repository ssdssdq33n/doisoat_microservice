package com.devteria.partnersession.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.SessionSalesProMonth;

@Repository
public interface SessionSalesProMonthRepository extends JpaRepository<SessionSalesProMonth, Long> {

    @Query(
            "SELECT s FROM SessionSalesProMonth s WHERE s.dateControl BETWEEN :startDate AND :endDate AND s.status = 'Thành công' "
                    + "AND s.dateCreated = (SELECT MAX(s2.dateCreated) FROM SessionSalesProMonth s2 WHERE s2.dateControl = s.dateControl AND s2.status = 'Thành công')")
    List<SessionSalesProMonth> findLatestSessionsByControlDate(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    Optional<SessionSalesProMonth> findSessionMonthByNameContaining(String name);

    List<SessionSalesProMonth> findAllByStatusNot(String status);

    Optional<SessionSalesProMonth> findSessionMonthByStatusEquals(String status);

    List<SessionSalesProMonth> findAllByNameContains(String id);

    Optional<SessionSalesProMonth> findTopByStatusEquals(String status);

    @Query("SELECT t FROM SessionSalesProMonth t WHERE t.status IN ('Đang xử lý', 'Chờ xử lý')")
    List<SessionSalesProMonth> findTransactionsWithSuccessOrFailureStatus();
}
