package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BookingMetricsService - Unit Tests")
class BookingMetricsServiceTest {

    @Test
    @DisplayName("Should record counters gauges and latency")
    void shouldRecordCountersGaugesAndLatency() {
        BookingMetricsService service = new BookingMetricsService(new SimpleMeterRegistry());

        service.recordBookingRequest();
        assertEquals(1.0, service.getBookingRequestsTotal());
        assertEquals(1, service.getActiveOperations());

        service.recordBookingSuccess();
        assertEquals(1.0, service.getBookingSuccessTotal());
        assertEquals(0, service.getActiveOperations());

        service.recordBookingRequest();
        service.recordBookingRejected();
        assertEquals(1.0, service.getBookingRejectedTotal());
        assertEquals(0, service.getActiveOperations());

        service.recordDoubleBooking();
        service.recordSessionCancellation();
        service.recordSessionReschedule();
        service.recordLatency(25);
        Timer.Sample sample = service.startLatencyMeasurement();
        long elapsed = service.stopLatencyMeasurement(sample);

        assertEquals(1.0, service.getDoubleBookingTotal());
        assertEquals(1.0, service.getSessionCancellationsTotal());
        assertEquals(1.0, service.getSessionReschedulesTotal());
        assertTrue(elapsed >= 0);
    }
}
