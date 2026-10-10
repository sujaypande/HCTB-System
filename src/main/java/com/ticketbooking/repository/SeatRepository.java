package com.ticketbooking.repository;

import com.ticketbooking.entity.Seat;
import com.ticketbooking.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventIdOrderByIdAsc(Long eventId);

    List<Seat> findByEventIdAndStatusOrderByIdAsc(Long eventId, SeatStatus status);

    boolean existsByEventIdAndSeatNumber(Long eventId, String seatNumber);

    /**
     * Locks the seat rows (SELECT ... FOR UPDATE) until the booking transaction ends, so two
     * concurrent requests for the same seat run one after the other. Ordered by id so that requests
     * locking overlapping seats always lock in the same order and cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id in :ids order by s.id")
    List<Seat> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from Seat s where s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
