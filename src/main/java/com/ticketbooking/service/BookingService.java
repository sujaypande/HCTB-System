package com.ticketbooking.service;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.dto.BookingResponse;
import com.ticketbooking.entity.Booking;
import com.ticketbooking.entity.BookingStatus;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.Seat;
import com.ticketbooking.entity.User;
import com.ticketbooking.exception.BadRequestException;
import com.ticketbooking.exception.ConflictException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final UserService userService;
    private final EventService eventService;

    public BookingService(BookingRepository bookingRepository, SeatRepository seatRepository,
                          UserService userService, EventService eventService) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.userService = userService;
        this.eventService = eventService;
    }

    /** Books all requested seats or none: any failure rolls back the whole transaction. */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        User user = userService.findUser(request.userId());
        Event event = eventService.findEvent(request.eventId());

        Set<Long> seatIds = new LinkedHashSet<>(request.seatIds());
        if (seatIds.size() != request.seatIds().size()) {
            throw new BadRequestException("Duplicate seat ids in request");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            Set<Long> found = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            seatIds.removeAll(found);
            throw new ResourceNotFoundException("Seat", seatIds.iterator().next());
        }

        for (Seat seat : seats) {
            if (!seat.getEvent().getId().equals(event.getId())) {
                throw new BadRequestException("Seat " + seat.getId() + " does not belong to event " + event.getId());
            }
        }

        List<String> unavailable = seats.stream().filter(s -> !s.isAvailable()).map(Seat::getSeatNumber).toList();
        if (!unavailable.isEmpty()) {
            throw new ConflictException("Seats already booked: " + String.join(", ", unavailable));
        }

        seats.forEach(Seat::book);
        Booking booking = bookingRepository.save(new Booking(user, event, seats));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking " + bookingId + " is already cancelled");
        }
        booking.getSeats().forEach(Seat::release);
        booking.cancel();
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(BookingResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        userService.findUser(userId);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }
}
