// 1,000 virtual users try to book the SAME seat at the same moment.
// Expected: exactly 1 booking succeeds (201), the other 999 get 409 Conflict, 0 other errors.
//
// Run (app must be up via docker compose):
//   docker run --rm -i --network highconcurrencyticketbookingsystem_default \
//     -e BASE_URL=http://app:8080 grafana/k6 run - < load-test/booking-load-test.js

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERS = 1000;
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

const bookingSuccess = new Counter('booking_success');
const bookingConflict = new Counter('booking_conflict');
const bookingOtherError = new Counter('booking_other_error');
const bookingDuration = new Trend('booking_duration', true);   // booking requests only, not setup

export const options = {
    setupTimeout: '5m',
    scenarios: {
        same_seat_rush: {
            executor: 'per-vu-iterations',   // every virtual user sends exactly one booking
            vus: USERS,
            iterations: 1,
            maxDuration: '5m',
        },
    },
    thresholds: {
        booking_success: ['count==1'],        // exactly one winner
        booking_other_error: ['count==0'],    // nothing besides 201 / 409
    },
};

// Runs once before the rush: one event, one seat, 1,000 users.
export function setup() {
    const runId = Date.now();
    const event = http.post(`${BASE_URL}/api/events`, JSON.stringify({
        name: `Load test ${runId}`, venue: 'Arena', eventDate: '2030-01-01T19:00:00',
    }), JSON_HEADERS).json();

    const seat = http.post(`${BASE_URL}/api/events/${event.id}/seats`, JSON.stringify({
        seats: [{ seatNumber: 'A1', price: 500 }],
    }), JSON_HEADERS).json()[0];

    const userIds = [];
    for (let start = 0; start < USERS; start += 100) {
        const batch = [];
        for (let i = start; i < start + 100; i++) {
            batch.push(['POST', `${BASE_URL}/api/users`,
                JSON.stringify({ name: `User ${i}`, email: `user${i}.${runId}@test.com` }), JSON_HEADERS]);
        }
        http.batch(batch).forEach((res) => userIds.push(res.json().id));
    }
    return { eventId: event.id, seatId: seat.id, userIds };
}

export default function (data) {
    const res = http.post(`${BASE_URL}/api/bookings`, JSON.stringify({
        userId: data.userIds[__VU - 1], eventId: data.eventId, seatIds: [data.seatId],
    }), JSON_HEADERS);

    bookingDuration.add(res.timings.duration);
    if (res.status === 201) bookingSuccess.add(1);
    else if (res.status === 409) bookingConflict.add(1);
    else bookingOtherError.add(1);

    check(res, { 'status is 201 or 409': (r) => r.status === 201 || r.status === 409 });
}

export function teardown(data) {
    const booked = http.get(`${BASE_URL}/api/events/${data.eventId}/seats?status=BOOKED`).json();
    console.log(`Seat A1 status after the rush: ${booked.length === 1 ? 'BOOKED' : 'NOT BOOKED'}`);
}
