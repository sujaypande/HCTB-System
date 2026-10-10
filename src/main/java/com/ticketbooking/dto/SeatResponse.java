package com.ticketbooking.dto;

import com.ticketbooking.entity.Seat;
import com.ticketbooking.entity.SeatStatus;

import java.math.BigDecimal;

public record SeatResponse(Long id, String seatNumber, BigDecimal price, SeatStatus status) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getPrice(), seat.getStatus());
    }
}
