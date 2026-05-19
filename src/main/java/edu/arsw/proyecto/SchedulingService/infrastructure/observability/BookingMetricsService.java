package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

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

    public BookingMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

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
}
