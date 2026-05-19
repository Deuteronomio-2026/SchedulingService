package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ObservabilityFacade {

    private final LoggingService loggingService;
    private final BookingMetricsService metricsService;
    private final KPIService kpiService;

    public ObservabilityFacade(LoggingService loggingService,
                                BookingMetricsService metricsService,
                                KPIService kpiService) {
        this.loggingService = loggingService;
        this.metricsService = metricsService;
        this.kpiService = kpiService;
    }

    // Booking Creation Flow
    public void recordBookingAttempt() {
        metricsService.recordBookingRequest();
    }

    public void recordSuccessfulBooking(UUID bookingId, UUID patientId, UUID psychologistId,
                                         String modality, long latencyMs) {
        metricsService.recordBookingSuccess();
        metricsService.recordLatency(latencyMs);
        loggingService.logBookingCreated(bookingId, patientId, psychologistId, modality, latencyMs);
    }

    public void recordRejectedBooking(UUID psychologistId, String date, String startTime) {
        metricsService.recordBookingRejected();
        loggingService.logSlotNotAvailable(psychologistId, date, startTime);
    }

    public void recordDoubleBookingDetected(UUID psychologistId, UUID conflictingSessionId,
                                             UUID acceptedBookingId, UUID rejectedBookingId,
                                             long resolutionTimeMs) {
        metricsService.recordDoubleBooking();
        loggingService.logDoubleBookingDetected(psychologistId, conflictingSessionId,
                acceptedBookingId, rejectedBookingId, resolutionTimeMs);
    }

    // Session Cancellation Flow
    public void recordSessionCancellation(UUID sessionId, UUID patientId, UUID psychologistId,
                                           String actorRole, String cancellationReason) {
        metricsService.recordSessionCancellation();
        loggingService.logSessionCancelled(sessionId, patientId, psychologistId,
                actorRole, cancellationReason);
    }

    // Session Reschedule Flow
    public void recordSessionReschedule(UUID sessionId, UUID patientId, UUID psychologistId,
                                         String previousDate, String newDate, String actorRole,
                                         long latencyMs) {
        metricsService.recordSessionReschedule();
        metricsService.recordLatency(latencyMs);
        loggingService.logSessionRescheduled(sessionId, patientId, psychologistId,
                previousDate, newDate, actorRole, latencyMs);
    }

    // Error Handling Flow
    public void recordBookingError(UUID patientId, UUID psychologistId, String errorType,
                                    String errorMessage, String stackTrace) {
        metricsService.recordBookingRejected();
        loggingService.logBookingError(patientId, psychologistId, errorType, errorMessage, stackTrace);
    }

    // Latency Measurement
    public Timer.Sample startLatencyMeasurement() {
        return metricsService.startLatencyMeasurement();
    }

    public long stopLatencyMeasurement(Timer.Sample sample) {
        return metricsService.stopLatencyMeasurement(sample);
    }

    // KPI Access
    public KPIService.KPISummary getKPISummary() {
        return kpiService.getSummary();
    }

    public double getSuccessRate() {
        return kpiService.getSuccessRate();
    }

    public double getRejectionRate() {
        return kpiService.getRejectionRate();
    }

    public double getCancellationRate() {
        return kpiService.getCancellationRate();
    }

    public double getRescheduleRate() {
        return kpiService.getRescheduleRate();
    }

    public double getDoubleBookingConflictRate() {
        return kpiService.getDoubleBookingConflictRate();
    }

    public int getActiveOperations() {
        return kpiService.getActiveOperations();
    }
}
