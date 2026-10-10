package com.ticketbooking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 150) String venue,
        @NotNull @Future LocalDateTime eventDate,
        @Size(max = 500) String description) {
}
