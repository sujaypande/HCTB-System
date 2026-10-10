package com.ticketbooking.dto;

import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.BookingStatus;
import com.ticketbooking.entity.Seat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(Long id, Long userId, Long eventId, String eventName, List<String> seatNumbers,
                              BigDecimal totalAmount, BookingStatus status, LocalDateTime createdAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getEvent().getId(),
                booking.getEvent().getName(),
                booking.getSeats().stream().map(Seat::getSeatNumber).toList(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}
