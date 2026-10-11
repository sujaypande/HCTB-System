// Throughput test: bookings arrive at a fixed rate, each one for a DIFFERENT seat (no lock contention).
// Expected: every booking succeeds (201); the achieved rate shows how many bookings/s the system sustains.
//
// Run (RATE = bookings per second, DURATION = seconds):
//   docker run --rm -i --network highconcurrencyticketbookingsystem_default \
//     -e BASE_URL=http://app:8080 -e RATE=100 -e DURATION=60 grafana/k6 run - < load-test/throughput-test.js

import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 50);
const DURATION = Number(__ENV.DURATION || 60);
const SEATS = RATE * DURATION + 100;
const USERS = 50;
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

const bookingSuccess = new Counter('booking_success');
const bookingFailed = new Counter('booking_failed');
const bookingDuration = new Trend('booking_duration', true);

export const options = {
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    setupTimeout: '10m',
    scenarios: {
        steady_rate: {
            executor: 'constant-arrival-rate',   // new bookings start at RATE/s whether or not earlier ones finished
            rate: RATE,
            timeUnit: '1s',
            duration: `${DURATION}s`,
            preAllocatedVUs: Math.min(RATE * 2, 500),
            maxVUs: 2000,
        },
    },
    thresholds: {
        booking_failed: ['count==0'],
    },
};

// One event with enough seats for every booking, plus a small pool of users.
export function setup() {
    const runId = Date.now();
    const event = http.post(`${BASE_URL}/api/events`, JSON.stringify({
        name: `Throughput ${runId}`, venue: 'Arena', eventDate: '2030-01-01T19:00:00',
    }), JSON_HEADERS).json();

    const seatIds = [];
    for (let start = 0; start < SEATS; start += 1000) {
        const seats = [];
        for (let i = start; i < Math.min(start + 1000, SEATS); i++) {
            seats.push({ seatNumber: `S${i}`, price: 500 });
        }
        const res = http.post(`${BASE_URL}/api/events/${event.id}/seats`, JSON.stringify({ seats }), JSON_HEADERS);
        res.json().forEach((s) => seatIds.push(s.id));
    }

    const batch = [];
    for (let i = 0; i < USERS; i++) {
        batch.push(['POST', `${BASE_URL}/api/users`,
            JSON.stringify({ name: `User ${i}`, email: `tp${i}.${runId}@test.com` }), JSON_HEADERS]);
    }
    const userIds = http.batch(batch).map((res) => res.json().id);
    return { eventId: event.id, seatIds, userIds };
}

export default function (data) {
    const i = exec.scenario.iterationInTest;
    if (i >= data.seatIds.length) return;

    const res = http.post(`${BASE_URL}/api/bookings`, JSON.stringify({
        userId: data.userIds[i % data.userIds.length], eventId: data.eventId, seatIds: [data.seatIds[i]],
    }), JSON_HEADERS);

    bookingDuration.add(res.timings.duration);
    if (res.status === 201) bookingSuccess.add(1);
    else bookingFailed.add(1);
    check(res, { 'status is 201': (r) => r.status === 201 });
}

export function handleSummary(data) {
    const ok = data.metrics.booking_success ? data.metrics.booking_success.values.count : 0;
    const dropped = data.metrics.dropped_iterations ? data.metrics.dropped_iterations.values.count : 0;
    const line = `\nTarget ${RATE}/s for ${DURATION}s -> achieved ${(ok / DURATION).toFixed(1)} bookings/s `
        + `(${ok} ok, dropped ${dropped})\n`;
    return { stdout: textSummary(data, { indent: ' ', enableColors: false }) + line };
}
