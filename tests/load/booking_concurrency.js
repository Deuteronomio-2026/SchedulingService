import http from 'k6/http';
import { check } from 'k6';
const BASE_URL = __ENV.BASE_URL;

import { Counter, Trend } from 'k6/metrics';

const PSYCHOLOGIST_ID = __ENV.PSYCHOLOGIST_ID || '550e8400-e29b-41d4-a716-446655440002';
const SLOT_DATE = __ENV.SLOT_DATE || '2030-01-15';
const SLOT_START_TIME = __ENV.SLOT_START_TIME || '14:00';
const SLOT_END_TIME = __ENV.SLOT_END_TIME || '15:00';
const SLOT_ID = __ENV.SLOT_ID || `${PSYCHOLOGIST_ID}:${SLOT_DATE}:${SLOT_START_TIME}`;
const RESET_SESSIONS = (__ENV.RESET_SESSIONS || 'false').toLowerCase() === 'true';

const bookingSuccesses = new Counter('booking_successes');
const bookingConflicts = new Counter('booking_conflicts');
const bookingBadRequests = new Counter('booking_bad_requests');
const bookingServerErrors = new Counter('booking_server_errors');
const bookingUnexpectedResponses = new Counter('booking_unexpected_responses');
const bookingLatency = new Trend('booking_latency', true);

export const options = {
  scenarios: {
    booking_conflict: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '30s',
      gracefulStop: '0s',
    },
  },
  thresholds: {
    booking_successes: ['count==1'],
    booking_conflicts: ['count==49'],
    booking_bad_requests: ['count==0'],
    booking_server_errors: ['count==0'],
    booking_unexpected_responses: ['count==0'],
    booking_latency: ['max<500'],
    checks: ['rate==1.0'],
  },
  summaryTrendStats: ['min', 'avg', 'med', 'p(95)', 'max'],
};

export function setup() {
  if (!BASE_URL) {
    throw new Error('BASE_URL is required. Example: BASE_URL=<url-publica-del-servicio> k6 run tests/load/booking_concurrency.js');
  }

  console.log(`Booking concurrency target: ${apiUrl('/sessions')}`);
  console.log(`Fixed slotId: ${SLOT_ID}`);

  if (RESET_SESSIONS) {
    const cleanup = http.del(apiUrl('/sessions'));
    if (cleanup.status >= 500) {
      throw new Error(`Session cleanup failed with HTTP ${cleanup.status}`);
    }
  }
}

export default function () {
  const bookingRequestBody = {
    patientId: patientIdForVu(),
    psychologistId: PSYCHOLOGIST_ID,
    date: SLOT_DATE,
    startTime: SLOT_START_TIME,
    endTime: SLOT_END_TIME,
    type: 'VIRTUAL',
    attentionType: 'PRIMERA_VEZ',
  };

  const response = http.post(apiUrl('/sessions'), JSON.stringify(bookingRequestBody), {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: {
      endpoint: 'booking',
      slot_id: SLOT_ID,
    },
  });

  const isSuccess = response.status === 201;
  const isConflict = response.status === 409;
  const isBadRequest = response.status === 400;
  const isServerError = response.status >= 500 && response.status <= 599;
  const isUnexpected = !isSuccess && !isConflict && !isBadRequest && !isServerError;

  bookingSuccesses.add(isSuccess ? 1 : 0);
  bookingConflicts.add(isConflict ? 1 : 0);
  bookingBadRequests.add(isBadRequest ? 1 : 0);
  bookingServerErrors.add(isServerError ? 1 : 0);
  bookingUnexpectedResponses.add(isUnexpected ? 1 : 0);
  bookingLatency.add(response.timings.duration);

  check(response, {
    'status is 201 or 409': () => isSuccess || isConflict,
    'no 5xx response': () => !isServerError,
    'latency is below 500ms': (res) => res.timings.duration < 500,
  });
}

export function handleSummary(data) {
  const successes = metricValue(data, 'booking_successes', 'count');
  const conflicts = metricValue(data, 'booking_conflicts', 'count');
  const badRequests = metricValue(data, 'booking_bad_requests', 'count');
  const serverErrors = metricValue(data, 'booking_server_errors', 'count');
  const unexpectedResponses = metricValue(data, 'booking_unexpected_responses', 'count');
  const maxLatency = metricValue(data, 'booking_latency', 'max');

  return {
    stdout: [
      '',
      'Booking concurrency summary',
      `slotId: ${SLOT_ID}`,
      `successful reservations: ${successes} (expected exactly 1)`,
      `conflicts: ${conflicts} (expected 49)`,
      `400 responses: ${badRequests} (expected 0)`,
      `5xx responses: ${serverErrors} (expected 0)`,
      `unexpected responses: ${unexpectedResponses} (expected 0)`,
      `max latency: ${maxLatency.toFixed(2)} ms (SLO < 500 ms)`,
      '',
    ].join('\n'),
  };
}

function patientIdForVu() {
  const vuSuffix = String(__VU).padStart(12, '0');
  return `00000000-0000-4000-8000-${vuSuffix}`;
}

function apiUrl(path) {
  return `${BASE_URL.replace(/\/$/, '')}${path}`;
}

function metricValue(data, metricName, valueName) {
  return data.metrics[metricName]?.values?.[valueName] || 0;
}
