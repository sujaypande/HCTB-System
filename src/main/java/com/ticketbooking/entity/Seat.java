package com.ticketbooking.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "seat_number"}))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    protected Seat() {
    }

    public Seat(Event event, String seatNumber, BigDecimal price) {
        this.event = event;
        this.seatNumber = seatNumber;
        this.price = price;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public void book() {
        this.status = SeatStatus.BOOKED;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getSeatNumber() { return seatNumber; }
    public BigDecimal getPrice() { return price; }
    public SeatStatus getStatus() { return status; }
}
