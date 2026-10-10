package com.ticketbooking.controller;

import com.ticketbooking.dto.BookingResponse;
import com.ticketbooking.dto.UserRequest;
import com.ticketbooking.dto.UserResponse;
import com.ticketbooking.service.BookingService;
import com.ticketbooking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;

    public UserController(UserService userService, BookingService bookingService) {
        this.userService = userService;
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping("/{id}/bookings")
    public List<BookingResponse> getUserBookings(@PathVariable Long id) {
        return bookingService.getUserBookings(id);
    }
}
