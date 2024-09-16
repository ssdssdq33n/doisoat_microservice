package com.devteria.partnersession.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.SessionProSupplyMonth;

@Repository
public interface SessionProSupplyMonthRepository extends JpaRepository<SessionProSupplyMonth, Long> {
    @Query(
            "SELECT s FROM SessionProSupplyMonth s WHERE s.dateControl BETWEEN :startDate AND :endDate AND s.status = 'Thành công' "
                    + "AND s.dateCreated = (SELECT MAX(s2.dateCreated) FROM SessionProSupplyMonth s2 WHERE s2.dateControl = s.dateControl AND s2.status = 'Thành công')")
    List<SessionProSupplyMonth> findLatestSessionsByControlDate(
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<SessionProSupplyMonth> findAllByDateControlBetween(LocalDateTime startDate, LocalDateTime endDate);

    Optional<SessionProSupplyMonth> findSessionProSupplyMonthsByNameContaining(String name);

    List<SessionProSupplyMonth> findAllByStatusNot(String status);

    Optional<SessionProSupplyMonth> findSessionProSupplyMonthsByStatusEquals(String status);

    List<SessionProSupplyMonth> findAllByNameContains(String id);

    Optional<SessionProSupplyMonth> findTopByStatusEquals(String status);

    @Query("SELECT t FROM SessionProSupplyMonth t WHERE t.status IN ('Đang xử lý', 'Chờ xử lý')")
    List<SessionProSupplyMonth> findTransactionsWithSuccessOrFailureStatus();
}
