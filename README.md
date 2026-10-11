# High-Concurrency Ticket Booking System

REST backend for booking event seats, built with Java 17, Spring Boot 3, Spring Data JPA and MySQL.
The main focus is making sure a seat can never be booked twice, even when many users try to book it
at the same time.

## Architecture

```
Controller   (UserController, EventController, BookingController)  - routing, @Valid request validation
   |
Service      (UserService, EventService, BookingService)            - business rules, @Transactional
   |
Repository   (Spring Data JPA)                                      - database access
   |
MySQL        (users, events, seats, bookings, booking_seats)
```

`GlobalExceptionHandler` (`@RestControllerAdvice`) turns every error into the same JSON format.

## Data model

| Table | Columns |
|---|---|
| `users` | id, name, email (unique) |
| `events` | id, name, venue, event_date, description |
| `seats` | id, event_id, seat_number, price, status (AVAILABLE / BOOKED), unique (event_id, seat_number) |
| `bookings` | id, user_id, event_id, total_amount, status (CONFIRMED / CANCELLED), created_at |
| `booking_seats` | booking_id, seat_id |

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/users` | Create user |
| GET | `/api/users/{id}` | Get user |
| GET | `/api/users/{id}/bookings` | Booking history |
| POST | `/api/events` | Create event |
| GET | `/api/events` | List events |
| GET | `/api/events/{id}` | Get event |
| PUT | `/api/events/{id}` | Update event |
| DELETE | `/api/events/{id}` | Delete event (only if it has no bookings) |
| POST | `/api/events/{id}/seats` | Add seats to an event |
| GET | `/api/events/{id}/seats?status=AVAILABLE` | Seat availability (status is optional) |
| POST | `/api/bookings` | Book seats |
| GET | `/api/bookings/{id}` | Get booking |
| POST | `/api/bookings/{id}/cancel` | Cancel booking, seats become available again |

Error response:

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

Status codes: 201 created, 400 invalid input, 404 not found, 409 seat already booked / duplicate data /
already cancelled, 500 unexpected error.

## How double booking is prevented

`BookingService.createBooking` runs in a single transaction:

1. Load the user and the event (404 if missing).
2. Lock the requested seat rows with `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`). Rows are locked
   in id order, so two requests for overlapping seats can't deadlock.
3. Check that every seat belongs to the event and is `AVAILABLE`, otherwise return 400 / 409.
4. Mark the seats `BOOKED`, save the booking, commit.

A second request for the same seat waits at step 2 until the first one commits, then sees `BOOKED`
and gets 409. Booking several seats is all or nothing. Cancelling locks the booking row the same way,
so two cancels of the same booking can't interfere.

Without the lock, `@Transactional` alone was not enough: in the concurrency test 20 parallel requests
created 10 bookings for the same seat.

## Testing

| Test | Result |
|---|---|
| `ConcurrentBookingTest`: 20 threads book one seat (JUnit + Testcontainers, real MySQL) | 10 bookings without the lock, 1 booking with it |
| `load-test/booking-load-test.js`: 1,000 k6 users book one seat at the same time | 1 success, 999 x 409, 0 errors |
| `load-test/throughput-test.js`: bookings at a fixed rate, each for a different seat | see AWS results below |
| `load-test/scaling-test.js`: steady read traffic to trigger auto scaling | see AWS results below |

```bash
mvn test        # needs Docker running
docker run --rm -i -e BASE_URL=http://<host> grafana/k6 run - < load-test/booking-load-test.js
docker run --rm -i -e BASE_URL=http://<host> -e RATE=100 -e DURATION=30 grafana/k6 run - < load-test/throughput-test.js
```

On my laptop the throughput was only about 30-36 bookings/s. Each MySQL commit waited ~300 ms for the
disk to sync, and turning off the per-commit sync for a quick check brought a single booking from
~300 ms down to ~16 ms, so the limit was the local disk, not the code.

## AWS deployment

```
k6 (EC2, same region) -> Application Load Balancer :80 -> Auto Scaling Group (EC2 t3.micro, Docker) :8080 -> RDS MySQL 8.4
```

- App instances are created from a launch template. Their user data (`deploy/ec2-user-data.sh`) installs
  Docker and runs the image from Docker Hub with the RDS connection details.
- Security groups are chained: the ALB accepts HTTP from the internet, the app instances accept 8080
  only from the ALB, and RDS accepts 3306 only from the app instances. RDS has no public access.
- The target group health check uses `GET /api/events`.
- The Auto Scaling Group uses a target tracking policy on average CPU (50%).

Results with one app instance (t3.micro) and RDS db.t4g.micro, k6 running on an EC2 instance in the same region:

| Test | Result |
|---|---|
| 1,000 users, one seat | 1 success, 999 x 409, 0 errors (p95 3.6 s, mostly waiting for the row lock) |
| 100 bookings/s, different seats | 0 errors, median 8.4 ms, p95 11 ms |
| 300 bookings/s, different seats | 0 errors, median 7.7 ms, p95 67 ms |
| 400 bookings/s, different seats | 388/s achieved, instance starts to saturate |
| 1,200 reads/s for ~10 min | CPU reached ~70%, the CloudWatch alarm fired and the group scaled from 1 to 2 instances; the new instance was healthy behind the ALB about 5 minutes later |

## Run locally

```bash
docker compose up -d --build
curl http://localhost:8080/api/events
```

If ports 8080 or 3307 are taken: `APP_PORT=8081 docker compose up -d`.

To run the app from the IDE / Maven against the MySQL container:

```bash
docker compose up -d mysql
mvn spring-boot:run      # uses localhost:3307, override with DB_URL / DB_USERNAME / DB_PASSWORD
```

Example requests:

```bash
curl -X POST localhost:8080/api/users  -H 'Content-Type: application/json' -d '{"name":"Asha","email":"asha@example.com"}'
curl -X POST localhost:8080/api/events -H 'Content-Type: application/json' -d '{"name":"Coldplay Live","venue":"DY Patil Stadium","eventDate":"2027-01-18T19:30:00"}'
curl -X POST localhost:8080/api/events/1/seats -H 'Content-Type: application/json' -d '{"seats":[{"seatNumber":"A1","price":1500},{"seatNumber":"A2","price":1500}]}'
curl "localhost:8080/api/events/1/seats?status=AVAILABLE"
curl -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{"userId":1,"eventId":1,"seatIds":[1,2]}'
curl -X POST localhost:8080/api/bookings/1/cancel
```
