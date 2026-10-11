// Auto-scaling test: steady read traffic (seat availability) for a long time, to push the app servers' CPU
// above the ASG target and watch new instances join.
//
// Run (RATE = requests per second, DURATION = seconds, EVENT_ID = an event that has seats):
//   docker run --rm -i -e BASE_URL=http://<alb-dns> -e RATE=300 -e DURATION=900 -e EVENT_ID=1 \
//     grafana/k6 run - < load-test/scaling-test.js

import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 300);
const DURATION = Number(__ENV.DURATION || 900);
const EVENT_ID = __ENV.EVENT_ID || '1';

export const options = {
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    scenarios: {
        steady_reads: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: `${DURATION}s`,
            preAllocatedVUs: Math.min(RATE, 300),
            maxVUs: 1000,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const res = http.get(`${BASE_URL}/api/events/${EVENT_ID}/seats`);
    check(res, { 'status is 200': (r) => r.status === 200 });
}
