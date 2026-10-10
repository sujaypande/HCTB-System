package com.ticketbooking;

import com.ticketbooking.dto.BookingRequest;
import com.ticketbooking.entity.Event;
import com.ticketbooking.entity.Seat;
import com.ticketbooking.entity.User;
import com.ticketbooking.exception.ConflictException;
import com.ticketbooking.repository.BookingRepository;
import com.ticketbooking.repository.EventRepository;
import com.ticketbooking.repository.SeatRepository;
import com.ticketbooking.repository.UserRepository;
import com.ticketbooking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Many users try to book the same seat at the same moment: exactly one may succeed. */
@SpringBootTest
@Testcontainers
class ConcurrentBookingTest {

    private static final int THREADS = 20;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withStartupTimeoutSeconds(600);

    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void onlyOneUserCanBookTheSameSeat() throws Exception {
        Event event = eventRepository.save(
                new Event("Concert", "Arena", LocalDateTime.now().plusDays(10), null));
        Seat seat = seatRepository.save(new Seat(event, "A1", new BigDecimal("500.00")));
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            userIds.add(userRepository.save(new User("User " + i, "user" + i + "@test.com")).getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger otherError = new AtomicInteger();

        for (Long userId : userIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.createBooking(
                            new BookingRequest(userId, event.getId(), List.of(seat.getId())));
                    success.incrementAndGet();
                } catch (ConflictException e) {
                    conflict.incrementAndGet();
                } catch (Exception e) {
                    otherError.incrementAndGet();
                }
                return null;
            });
        }

        // Release all threads at once so their requests really overlap.
        ready.await();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        System.out.printf("success=%d conflict=%d other=%d bookings=%d%n",
                success.get(), conflict.get(), otherError.get(), bookingRepository.count());
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(THREADS - 1);
        assertThat(bookingRepository.count()).isEqualTo(1);
    }
}
