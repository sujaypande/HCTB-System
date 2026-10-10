package com.ticketbooking.dto;

import com.ticketbooking.entity.Event;

import java.time.LocalDateTime;

public record EventResponse(Long id, String name, String venue, LocalDateTime eventDate, String description) {

    public static EventResponse from(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getVenue(),
                event.getEventDate(), event.getDescription());
    }
}
