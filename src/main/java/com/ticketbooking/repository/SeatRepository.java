package com.ticketbooking.repository;

import com.ticketbooking.entity.Seat;
import com.ticketbooking.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventIdOrderByIdAsc(Long eventId);

    List<Seat> findByEventIdAndStatusOrderByIdAsc(Long eventId, SeatStatus status);

    boolean existsByEventIdAndSeatNumber(Long eventId, String seatNumber);

    @Modifying
    @Query("delete from Seat s where s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
}
