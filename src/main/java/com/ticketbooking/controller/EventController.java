package com.ticketbooking.controller;

import com.ticketbooking.dto.CreateSeatsRequest;
import com.ticketbooking.dto.EventRequest;
import com.ticketbooking.dto.EventResponse;
import com.ticketbooking.dto.SeatResponse;
import com.ticketbooking.entity.SeatStatus;
import com.ticketbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody EventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping
    public List<EventResponse> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @PutMapping("/{id}")
    public EventResponse updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }

    @PostMapping("/{id}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatResponse> addSeats(@PathVariable Long id, @Valid @RequestBody CreateSeatsRequest request) {
        return eventService.addSeats(id, request);
    }

    /** Seat availability, e.g. GET /api/events/1/seats?status=AVAILABLE */
    @GetMapping("/{id}/seats")
    public List<SeatResponse> getSeats(@PathVariable Long id, @RequestParam(required = false) SeatStatus status) {
        return eventService.getSeats(id, status);
    }
}
