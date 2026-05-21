import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8082').replace(/\/$/, '');
const PSYCHOLOGIST_ID = __ENV.PSYCHOLOGIST_ID || '550e8400-e29b-41d4-a716-446655440002';
const SLOT_DATE = __ENV.SLOT_DATE || '2030-01-15';
const SLOT_START_TIME = __ENV.SLOT_START_TIME || '14:00';
const SLOT_END_TIME = __ENV.SLOT_END_TIME || '15:00';
const SLOT_ID = __ENV.SLOT_ID || `${PSYCHOLOGIST_ID}:${SLOT_DATE}:${SLOT_START_TIME}`;
const RESET_SESSIONS = (__ENV.RESET_SESSIONS || 'false').toLowerCase() === 'true';

const bookingSuccesses = new Counter('booking_successes');
const bookingConflicts = new Counter('booking_conflicts');
const bookingServerErrors = new Counter('booking_server_errors');
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
    booking_server_errors: ['count==0'],
    booking_latency: ['max<500'],
    checks: ['rate==1.0'],
  },
  summaryTrendStats: ['min', 'avg', 'med', 'p(95)', 'max'],
};

export function setup() {
  console.log(`Booking concurrency target: ${BASE_URL}/sessions`);
  console.log(`Fixed slotId: ${SLOT_ID}`);

  if (RESET_SESSIONS) {
    const cleanup = http.del(`${BASE_URL}/sessions`);
    if (cleanup.status >= 500) {
      throw new Error(`Session cleanup failed with HTTP ${cleanup.status}`);
    }
  }
}

export default function () {
  const response = http.post(`${BASE_URL}/sessions`, JSON.stringify({
    patientId: patientIdForVu(),
    psychologistId: PSYCHOLOGIST_ID,
    date: SLOT_DATE,
    startTime: SLOT_START_TIME,
    endTime: SLOT_END_TIME,
    type: 'VIRTUAL',
    attentionType: 'PRIMERA_VEZ',
  }), {
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
  const isServerError = response.status >= 500 && response.status <= 599;

  bookingSuccesses.add(isSuccess ? 1 : 0);
  bookingConflicts.add(isConflict ? 1 : 0);
  bookingServerErrors.add(isServerError ? 1 : 0);
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
  const serverErrors = metricValue(data, 'booking_server_errors', 'count');
  const maxLatency = metricValue(data, 'booking_latency', 'max');

  return {
    stdout: [
      '',
      'Booking concurrency summary',
      `slotId: ${SLOT_ID}`,
      `successful reservations: ${successes} (expected exactly 1)`,
      `conflicts: ${conflicts} (expected 49)`,
      `5xx responses: ${serverErrors} (expected 0)`,
      `max latency: ${maxLatency.toFixed(2)} ms (SLO < 500 ms)`,
      '',
    ].join('\n'),
  };
}

function patientIdForVu() {
  const vuSuffix = String(exec.vu.idInTest).padStart(12, '0');
  return `00000000-0000-4000-8000-${vuSuffix}`;
}

function metricValue(data, metricName, valueName) {
  return data.metrics[metricName]?.values?.[valueName] || 0;
}
