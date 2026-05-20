package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObservabilityFacade - Unit Tests")
class ObservabilityFacadeTest {

    @Mock
    private LoggingService loggingService;

    @Mock
    private BookingMetricsService metricsService;

    @Mock
    private KPIService kpiService;

    @Mock
    private Timer.Sample sample;

    private ObservabilityFacade facade;

    @BeforeEach
    void setUp() {
        facade = new ObservabilityFacade(loggingService, metricsService, kpiService);
    }

    @Test
    @DisplayName("Should record booking attempt")
    void shouldRecordBookingAttempt() {
        facade.recordBookingAttempt();

        verify(metricsService).recordBookingRequest();
    }

    @Test
    @DisplayName("Should record successful booking")
    void shouldRecordSuccessfulBooking() {
        UUID bookingId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();

        facade.recordSuccessfulBooking(bookingId, patientId, psychologistId, "VIRTUAL", 42);

        verify(metricsService).recordBookingSuccess();
        verify(metricsService).recordLatency(42);
        verify(loggingService).logBookingCreated(bookingId, patientId, psychologistId, "VIRTUAL", 42);
    }

    @Test
    @DisplayName("Should record rejected and double booking flows")
    void shouldRecordRejectedAndDoubleBookingFlows() {
        UUID psychologistId = UUID.randomUUID();
        UUID conflictingSessionId = UUID.randomUUID();
        UUID acceptedBookingId = UUID.randomUUID();
        UUID rejectedBookingId = UUID.randomUUID();

        facade.recordRejectedBooking(psychologistId, "2026-04-15", "14:00");
        facade.recordDoubleBookingDetected(
                psychologistId, conflictingSessionId, acceptedBookingId, rejectedBookingId, 9);

        verify(metricsService).recordBookingRejected();
        verify(loggingService).logSlotNotAvailable(psychologistId, "2026-04-15", "14:00");
        verify(metricsService).recordDoubleBooking();
        verify(loggingService).logDoubleBookingDetected(
                psychologistId, conflictingSessionId, acceptedBookingId, rejectedBookingId, 9);
    }

    @Test
    @DisplayName("Should record cancellation and reschedule flows")
    void shouldRecordCancellationAndRescheduleFlows() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();

        facade.recordSessionCancellation(sessionId, patientId, psychologistId, "SYSTEM", "Manual");
        facade.recordSessionReschedule(
                sessionId, patientId, psychologistId, "2026-04-15", "2026-04-16", "SYSTEM", 12);

        verify(metricsService).recordSessionCancellation();
        verify(loggingService).logSessionCancelled(sessionId, patientId, psychologistId, "SYSTEM", "Manual");
        verify(metricsService).recordSessionReschedule();
        verify(metricsService).recordLatency(12);
        verify(loggingService).logSessionRescheduled(
                sessionId, patientId, psychologistId, "2026-04-15", "2026-04-16", "SYSTEM", 12);
    }

    @Test
    @DisplayName("Should record errors and latency")
    void shouldRecordErrorsAndLatency() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        when(metricsService.startLatencyMeasurement()).thenReturn(sample);
        when(metricsService.stopLatencyMeasurement(sample)).thenReturn(17L);

        facade.recordBookingError(patientId, psychologistId, "IllegalStateException", "boom", "stack");
        Timer.Sample started = facade.startLatencyMeasurement();
        long elapsed = facade.stopLatencyMeasurement(sample);

        verify(metricsService).recordBookingRejected();
        verify(loggingService).logBookingError(patientId, psychologistId, "IllegalStateException", "boom", "stack");
        assertSame(sample, started);
        assertEquals(17L, elapsed);
    }

    @Test
    @DisplayName("Should expose KPI values")
    void shouldExposeKpiValues() {
        KPIService.KPISummary summary = new KPIService.KPISummary(
                1.0, 2.0, 3.0, 4.0, 5.0, 6, 7, 8.0, 9.0, 10.0, 11.0);
        when(kpiService.getSummary()).thenReturn(summary);
        when(kpiService.getSuccessRate()).thenReturn(1.0);
        when(kpiService.getRejectionRate()).thenReturn(2.0);
        when(kpiService.getCancellationRate()).thenReturn(4.0);
        when(kpiService.getRescheduleRate()).thenReturn(5.0);
        when(kpiService.getDoubleBookingConflictRate()).thenReturn(3.0);
        when(kpiService.getActiveOperations()).thenReturn(7);

        assertSame(summary, facade.getKPISummary());
        assertEquals(1.0, facade.getSuccessRate());
        assertEquals(2.0, facade.getRejectionRate());
        assertEquals(4.0, facade.getCancellationRate());
        assertEquals(5.0, facade.getRescheduleRate());
        assertEquals(3.0, facade.getDoubleBookingConflictRate());
        assertEquals(7, facade.getActiveOperations());
    }
}
