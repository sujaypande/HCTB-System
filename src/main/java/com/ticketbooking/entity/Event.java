package com.ticketbooking.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String venue;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    private String description;

    protected Event() {
    }

    public Event(String name, String venue, LocalDateTime eventDate, String description) {
        update(name, venue, eventDate, description);
    }

    public void update(String name, String venue, LocalDateTime eventDate, String description) {
        this.name = name;
        this.venue = venue;
        this.eventDate = eventDate;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getVenue() { return venue; }
    public LocalDateTime getEventDate() { return eventDate; }
    public String getDescription() { return description; }
}
