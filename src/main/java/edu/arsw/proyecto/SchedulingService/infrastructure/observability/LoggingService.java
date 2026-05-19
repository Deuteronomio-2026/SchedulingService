package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoggingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingService.class);
    private static final String SERVICE_NAME = "scheduling-service";

    // Events
    private static final String BOOKING_CREATED = "BOOKING_CREATED";
    private static final String BOOKING_ERROR = "BOOKING_ERROR";
    private static final String DOUBLE_BOOKING_DETECTED = "DOUBLE_BOOKING_DETECTED";
    private static final String SESSION_CANCELLED = "SESSION_CANCELLED";
    private static final String SESSION_RESCHEDULED = "SESSION_RESCHEDULED";

    // MDC Keys
    private static final String EVENT_MDC = "event";
    private static final String ACTOR_MDC = "actor";

    public void logBookingCreated(UUID bookingId, UUID patientId, UUID psychologistId,
                                   String modality, long latencyMs) {
        setEventMDC(BOOKING_CREATED, "SYSTEM");
        try {
            LOGGER.info(
                    "Booking created successfully",
                    StructuredArguments.kv("bookingId", bookingId),
                    StructuredArguments.kv("patientId", patientId),
                    StructuredArguments.kv("psychologistId", psychologistId),
                    StructuredArguments.kv("modality", modality),
                    StructuredArguments.kv("latencyMs", latencyMs),
                    StructuredArguments.kv("result", "SUCCESS")
            );
        } finally {
            clearEventMDC();
        }
    }

    public void logDoubleBookingDetected(UUID psychologistId, UUID conflictingSessionId,
                                          UUID acceptedBookingId, UUID rejectedBookingId,
                                          long resolutionTimeMs) {
        setEventMDC(DOUBLE_BOOKING_DETECTED, "SYSTEM");
        try {
            LOGGER.warn(
                    "Double booking detected and resolved",
                    StructuredArguments.kv("psychologistId", psychologistId),
                    StructuredArguments.kv("conflictingSessionId", conflictingSessionId),
                    StructuredArguments.kv("acceptedBookingId", acceptedBookingId),
                    StructuredArguments.kv("rejectedBookingId", rejectedBookingId),
                    StructuredArguments.kv("resolutionTimeMs", resolutionTimeMs),
                    StructuredArguments.kv("result", "RESOLVED")
            );
        } finally {
            clearEventMDC();
        }
    }

    public void logSessionCancelled(UUID sessionId, UUID patientId, UUID psychologistId,
                                     String actorRole, String cancellationReason) {
        setEventMDC(SESSION_CANCELLED, actorRole);
        try {
            LOGGER.info(
                    "Session cancelled",
                    StructuredArguments.kv("sessionId", sessionId),
                    StructuredArguments.kv("patientId", patientId),
                    StructuredArguments.kv("psychologistId", psychologistId),
                    StructuredArguments.kv("actorRole", actorRole),
                    StructuredArguments.kv("cancellationReason", cancellationReason),
                    StructuredArguments.kv("result", "SUCCESS")
            );
        } finally {
            clearEventMDC();
        }
    }

    public void logSessionRescheduled(UUID sessionId, UUID patientId, UUID psychologistId,
                                       String previousDate, String newDate, String actorRole,
                                       long latencyMs) {
        setEventMDC(SESSION_RESCHEDULED, actorRole);
        try {
            LOGGER.info(
                    "Session rescheduled",
                    StructuredArguments.kv("sessionId", sessionId),
                    StructuredArguments.kv("patientId", patientId),
                    StructuredArguments.kv("psychologistId", psychologistId),
                    StructuredArguments.kv("previousDate", previousDate),
                    StructuredArguments.kv("newDate", newDate),
                    StructuredArguments.kv("actorRole", actorRole),
                    StructuredArguments.kv("latencyMs", latencyMs),
                    StructuredArguments.kv("result", "SUCCESS")
            );
        } finally {
            clearEventMDC();
        }
    }

    public void logBookingError(UUID patientId, UUID psychologistId, String errorType,
                                 String errorMessage, String stackTrace) {
        setEventMDC(BOOKING_ERROR, "SYSTEM");
        try {
            LOGGER.error(
                    "Booking error occurred",
                    StructuredArguments.kv("patientId", patientId),
                    StructuredArguments.kv("psychologistId", psychologistId),
                    StructuredArguments.kv("errorType", errorType),
                    StructuredArguments.kv("errorMessage", errorMessage),
                    StructuredArguments.kv("stackTrace", stackTrace),
                    StructuredArguments.kv("result", "FAILED")
            );
        } finally {
            clearEventMDC();
        }
    }

    public void logSlotNotAvailable(UUID psychologistId, String date, String startTime) {
        setEventMDC("SLOT_NOT_AVAILABLE", "SYSTEM");
        try {
            LOGGER.warn(
                    "Slot not available",
                    StructuredArguments.kv("psychologistId", psychologistId),
                    StructuredArguments.kv("date", date),
                    StructuredArguments.kv("startTime", startTime),
                    StructuredArguments.kv("result", "REJECTED")
            );
        } finally {
            clearEventMDC();
        }
    }

    private void setEventMDC(String event, String actor) {
        MDC.put(EVENT_MDC, event);
        MDC.put(ACTOR_MDC, actor);
    }

    private void clearEventMDC() {
        MDC.remove(EVENT_MDC);
        MDC.remove(ACTOR_MDC);
    }
}
