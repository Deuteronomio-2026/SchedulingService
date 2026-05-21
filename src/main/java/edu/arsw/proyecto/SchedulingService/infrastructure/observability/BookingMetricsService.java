package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BookingMetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter bookingRequestsTotal;
    private final Counter bookingSuccessTotal;
    private final Counter bookingRejectedTotal;
    private final Counter doubleBookingTotal;
    private final Counter sessionCancellationsTotal;
    private final Counter sessionReschedulesTotal;
    private final Timer bookingLatencyTimer;
    private final AtomicInteger activeOperations;
    private final Timer bookingEndpointLatencyTimer;
    private final AtomicInteger activeBookingUsers;
    private final ConcurrentMap<String, Counter> bookingResponsesByStatus;

    public BookingMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.bookingResponsesByStatus = new ConcurrentHashMap<>();

        // Counters: Total requests
        this.bookingRequestsTotal = Counter.builder("booking_requests_total")
                .description("Total number of booking requests")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Counters: Successful bookings
        this.bookingSuccessTotal = Counter.builder("booking_success_total")
                .description("Total number of successful bookings")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Counters: Rejected bookings
        this.bookingRejectedTotal = Counter.builder("booking_rejected_total")
                .description("Total number of rejected bookings")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Counters: Double bookings detected
        this.doubleBookingTotal = Counter.builder("double_booking_total")
                .description("Total number of double bookings detected")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Counters: Session cancellations
        this.sessionCancellationsTotal = Counter.builder("session_cancellations_total")
                .description("Total number of session cancellations")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Counters: Session reschedules
        this.sessionReschedulesTotal = Counter.builder("session_reschedules_total")
                .description("Total number of session reschedules")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Timer: Booking latency
        this.bookingLatencyTimer = Timer.builder("booking_latency_ms")
                .description("Time taken to process booking requests in milliseconds")
                .tag("service", "scheduling-service")
                .register(meterRegistry);

        // Gauge: Active operations
        this.activeOperations = new AtomicInteger(0);
        meterRegistry.gauge("active_scheduling_operations",
                activeOperations,
                AtomicInteger::get);

        // Endpoint-level metrics for the concurrent booking SLO.
        this.bookingEndpointLatencyTimer = Timer.builder("booking_request_duration")
                .description("HTTP latency for POST /sessions booking requests")
                .tag("endpoint", "/sessions")
                .tag("method", "POST")
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500),
                        Duration.ofSeconds(1))
                .register(meterRegistry);

        this.activeBookingUsers = new AtomicInteger(0);
        Gauge.builder("booking_concurrent_users_active", activeBookingUsers, AtomicInteger::get)
                .description("Active concurrent users executing POST /sessions")
                .tag("endpoint", "/sessions")
                .tag("method", "POST")
                .register(meterRegistry);
    }

    public void recordBookingRequest() {
        bookingRequestsTotal.increment();
        activeOperations.incrementAndGet();
    }

    public void recordBookingSuccess() {
        bookingSuccessTotal.increment();
        activeOperations.decrementAndGet();
    }

    public void recordBookingRejected() {
        bookingRejectedTotal.increment();
        activeOperations.decrementAndGet();
    }

    public void recordDoubleBooking() {
        doubleBookingTotal.increment();
    }

    public void recordSessionCancellation() {
        sessionCancellationsTotal.increment();
    }

    public void recordSessionReschedule() {
        sessionReschedulesTotal.increment();
    }

    public void recordLatency(long latencyMs) {
        bookingLatencyTimer.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public Timer.Sample startLatencyMeasurement() {
        return Timer.start(meterRegistry);
    }

    public long stopLatencyMeasurement(Timer.Sample sample) {
        long durationNanos = sample.stop(bookingLatencyTimer);
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(durationNanos);
    }

    public Timer.Sample startBookingEndpointRequest() {
        activeBookingUsers.incrementAndGet();
        return Timer.start(meterRegistry);
    }

    public void stopBookingEndpointRequest(Timer.Sample sample, int statusCode) {
        try {
            if (sample != null) {
                sample.stop(bookingEndpointLatencyTimer);
            }
            recordBookingResponseStatus(statusCode);
        } finally {
            activeBookingUsers.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    public void recordBookingResponseStatus(int statusCode) {
        String status = normalizeBookingStatus(statusCode);
        bookingResponsesByStatus
                .computeIfAbsent(status, this::bookingResponseCounter)
                .increment();
    }

    private Counter bookingResponseCounter(String status) {
        return Counter.builder("booking_responses")
                .description("HTTP responses for POST /sessions grouped by status")
                .tag("endpoint", "/sessions")
                .tag("method", "POST")
                .tag("status", status)
                .register(meterRegistry);
    }

    private String normalizeBookingStatus(int statusCode) {
        if (statusCode >= 500 && statusCode <= 599) {
            return "5xx";
        }
        return Integer.toString(statusCode);
    }

    // Getters for metrics access
    public double getBookingRequestsTotal() {
        return bookingRequestsTotal.count();
    }

    public double getBookingSuccessTotal() {
        return bookingSuccessTotal.count();
    }

    public double getBookingRejectedTotal() {
        return bookingRejectedTotal.count();
    }

    public double getDoubleBookingTotal() {
        return doubleBookingTotal.count();
    }

    public double getSessionCancellationsTotal() {
        return sessionCancellationsTotal.count();
    }

    public double getSessionReschedulesTotal() {
        return sessionReschedulesTotal.count();
    }

    public int getActiveOperations() {
        return activeOperations.get();
    }

    public int getActiveBookingUsers() {
        return activeBookingUsers.get();
    }

    public double getBookingResponsesTotal(String status) {
        Counter counter = bookingResponsesByStatus.get(status);
        return counter == null ? 0.0 : counter.count();
    }
}
