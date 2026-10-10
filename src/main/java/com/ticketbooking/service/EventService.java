package com.ticketbooking.service;

import com.ticketbooking.dto.CreateSeatsRequest;
import com.ticketbooking.dto.EventRequest;
import com.ticketbooking.dto.EventResponse;
import com.ticketbooking.dto.SeatRequest;
import com.ticketbooking.dto.SeatResponse;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.Seat;
import com.ticketbooking.entity.SeatStatus;
import com.ticketbooking.exception.ConflictException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    public EventService(EventRepository eventRepository, SeatRepository seatRepository,
                        BookingRepository bookingRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = new Event(request.name().trim(), request.venue().trim(), request.eventDate(),
                request.description());
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(EventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        return EventResponse.from(findEvent(id));
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = findEvent(id);
        event.update(request.name().trim(), request.venue().trim(), request.eventDate(), request.description());
        return EventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = findEvent(id);
        if (bookingRepository.existsByEventId(id)) {
            throw new ConflictException("Event " + id + " has bookings and cannot be deleted");
        }
        seatRepository.deleteByEventId(id);
        eventRepository.delete(event);
    }

    @Transactional
    public List<SeatResponse> addSeats(Long eventId, CreateSeatsRequest request) {
        Event event = findEvent(eventId);
        Set<String> seenInRequest = new HashSet<>();
        List<Seat> seats = request.seats().stream().map(s -> toSeat(event, s, seenInRequest)).toList();
        return seatRepository.saveAll(seats).stream().map(SeatResponse::from).toList();
    }

    /** Seat availability: all seats of the event, or only those in the given status. */
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(Long eventId, SeatStatus status) {
        findEvent(eventId);
        List<Seat> seats = status == null
                ? seatRepository.findByEventIdOrderByIdAsc(eventId)
                : seatRepository.findByEventIdAndStatusOrderByIdAsc(eventId, status);
        return seats.stream().map(SeatResponse::from).toList();
    }

    Event findEvent(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    private Seat toSeat(Event event, SeatRequest request, Set<String> seenInRequest) {
        String seatNumber = request.seatNumber().trim().toUpperCase();
        if (!seenInRequest.add(seatNumber) || seatRepository.existsByEventIdAndSeatNumber(event.getId(), seatNumber)) {
            throw new ConflictException("Seat " + seatNumber + " already exists for event " + event.getId());
        }
        return new Seat(event, seatNumber, request.price());
    }
}
