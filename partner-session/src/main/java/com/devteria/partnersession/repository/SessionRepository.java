package com.devteria.partnersession.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.devteria.partnersession.model.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query(
            "SELECT s FROM Session s WHERE s.dateControl BETWEEN :startDate AND :endDate AND s.status = 'Thành công' "
                    + "AND s.dateCreated = (SELECT MAX(s2.dateCreated) FROM Session s2 WHERE s2.dateControl = s.dateControl AND s2.status = 'Thành công')")
    List<Session> findFirstSessionsForEachDayByControlDateBetweenAndStatus(Date startDate, Date endDate);

    @Query("SELECT s FROM Session s WHERE s.dateControl BETWEEN :startDate AND :endDate AND s.status = 'Thành công'")
    List<Session> findSessionsByControlDateBetweenAndStatus(Date startDate, Date endDate);

    @Query(
            value =
                    "SELECT s FROM Session s WHERE s.dateControl = :date AND s.status = 'Thành công' ORDER BY s.dateCreated ASC")
    List<Session> findFirstByControlDateAndStatusOrderByCreatedDate(Date date);

    List<Session> findByDateControlBetween(Date startDate, Date endDate);

    Optional<Session> findSessionByNameContaining(String name);

    List<Session> findAllByStatusNot(String status);

    Optional<Session> findSessionByStatusEquals(String status);

    //    @Query(
    //            "SELECT u FROM Session u WHERE u.status = :status AND ABS(u.SESSION_ID - :SESSION_ID) = (SELECT
    // MIN(ABS(u2.SESSION_ID - :SESSION_ID)) FROM Session u2 WHERE u2.status = :status)")
    //    Optional<Session> findNearestUserByIdAndStatus(@Param("id") Long SESSION_ID, @Param("status") String status);

    Optional<Session> findTopByStatusEquals(String status);

    List<Session> findAllByNameContains(String id);

    @Query("SELECT t FROM Session t WHERE t.status IN ('Đang xử lý', 'Chờ xử lý')")
    List<Session> findTransactionsWithSuccessOrFailureStatus();
}
