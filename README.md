# Ticket Booking System

RESTful ticket booking backend built with Java 17, Spring Boot 3, Spring Data JPA and MySQL,
containerized with Docker Compose.

## Architecture

```
HTTP request
   │
Controller   (UserController, EventController, BookingController)  – routing + @Valid request validation
   │
Service      (UserService, EventService, BookingService)            – business rules, @Transactional
   │
Repository   (Spring Data JPA interfaces)                           – database access
   │
MySQL        (users, events, seats, bookings, booking_seats)
```

Errors from any layer are turned into one JSON shape by `GlobalExceptionHandler` (`@RestControllerAdvice`).

## Data model

| Table           | Columns                                                                 |
|-----------------|-------------------------------------------------------------------------|
| `users`         | id, name, email (unique)                                                |
| `events`        | id, name, venue, event_date, description                                |
| `seats`         | id, event_id, seat_number, price, status (AVAILABLE/BOOKED) — unique (event_id, seat_number) |
| `bookings`      | id, user_id, event_id, total_amount, status (CONFIRMED/CANCELLED), created_at |
| `booking_seats` | booking_id, seat_id                                                     |

## API

| Method | Endpoint                               | Purpose                                   |
|--------|----------------------------------------|-------------------------------------------|
| POST   | `/api/users`                           | Create user                               |
| GET    | `/api/users/{id}`                      | Get user                                  |
| GET    | `/api/users/{id}/bookings`             | Booking history of a user                 |
| POST   | `/api/events`                          | Create event                              |
| GET    | `/api/events`                          | List events                               |
| GET    | `/api/events/{id}`                     | Get event                                 |
| PUT    | `/api/events/{id}`                     | Update event                              |
| DELETE | `/api/events/{id}`                     | Delete event (only if it has no bookings) |
| POST   | `/api/events/{id}/seats`               | Add seats to an event                     |
| GET    | `/api/events/{id}/seats?status=AVAILABLE` | Seat availability (status optional)    |
| POST   | `/api/bookings`                        | Book seats                                |
| GET    | `/api/bookings/{id}`                   | Get booking                               |
| POST   | `/api/bookings/{id}/cancel`            | Cancel booking, seats become AVAILABLE    |

Error response example:

```json
{
  "timestamp": "2026-10-07T18:06:47.13",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```

Status codes: 201 created, 400 validation/malformed, 404 not found, 409 seat already booked /
duplicate data / already cancelled, 500 unexpected.

## Booking consistency

`BookingService.createBooking` runs in one `@Transactional` method:

1. Load user and event (404 if missing).
2. Load the requested seats (404 if any is missing).
3. Check every seat belongs to the event and is `AVAILABLE`; otherwise throw (400/409).
4. Mark seats `BOOKED`, save the booking with the total price, commit.

If any step fails, the whole transaction rolls back, so booking several seats is all-or-nothing:
seat statuses and the booking are saved together or not at all.

## Run with Docker Compose

```bash
docker compose up -d --build
curl http://localhost:8080/api/events
```

If port 8080 or 3307 is taken on your machine: `APP_PORT=8081 docker compose up -d` (MySQL is exposed on host port 3307).

## Run locally (without the app container)

```bash
docker compose up -d mysql
mvn spring-boot:run          # connects to localhost:3307, override with DB_URL / DB_USERNAME / DB_PASSWORD
```

## Quick test

```bash
curl -X POST localhost:8080/api/users  -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com"}'
curl -X POST localhost:8080/api/events -H 'Content-Type: application/json' -d '{"name":"Coldplay Live","venue":"DY Patil Stadium","eventDate":"2027-01-18T19:30:00"}'
curl -X POST localhost:8080/api/events/1/seats -H 'Content-Type: application/json' -d '{"seats":[{"seatNumber":"A1","price":1500},{"seatNumber":"A2","price":1500}]}'
curl "localhost:8080/api/events/1/seats?status=AVAILABLE"
curl -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"userId":1,"eventId":1,"seatIds":[1,2]}'
curl -X POST localhost:8080/api/bookings/1/cancel
```

## Deploy on AWS EC2

1. Launch an EC2 instance (Amazon Linux 2023, t2.micro/t3.small). In the security group allow inbound
   SSH (22) from your IP and TCP 8080 from anywhere. Do **not** open 3306/3307.
2. SSH in and install Docker + Compose:
   ```bash
   sudo dnf install -y docker git
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user && newgrp docker
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   ```
3. Copy the project (git clone or `scp`), set real passwords, and start it:
   ```bash
   export MYSQL_ROOT_PASSWORD=change-me DB_PASSWORD=change-me-too
   docker compose up -d --build
   ```
4. Test: `curl http://<EC2_PUBLIC_IP>:8080/api/events`

`restart: unless-stopped` and the named volume `mysql-data` keep the app running and the data intact
across container and instance restarts.
