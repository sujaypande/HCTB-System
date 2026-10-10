package com.ticketbooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSeatsRequest(@NotEmpty List<@Valid SeatRequest> seats) {
}
