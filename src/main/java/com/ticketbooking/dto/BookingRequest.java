package com.ticketbooking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotNull Long userId,
        @NotNull Long eventId,
        @NotEmpty @Size(max = 10) List<@NotNull Long> seatIds) {
}
