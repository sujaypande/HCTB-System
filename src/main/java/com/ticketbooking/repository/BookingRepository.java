package com.ticketbooking.repository;

import com.ticketbooking.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"event", "seats"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByEventId(Long eventId);
}
