package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("LoggingService - Unit Tests")
class LoggingServiceTest {

    private final LoggingService loggingService = new LoggingService();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should clear MDC after booking created log")
    void shouldClearMdcAfterBookingCreatedLog() {
        loggingService.logBookingCreated(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "VIRTUAL", 25);

        assertEventContextCleared();
    }

    @Test
    @DisplayName("Should clear MDC after double booking log")
    void shouldClearMdcAfterDoubleBookingLog() {
        loggingService.logDoubleBookingDetected(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 11);

        assertEventContextCleared();
    }

    @Test
    @DisplayName("Should clear MDC after cancellation log")
    void shouldClearMdcAfterCancellationLog() {
        loggingService.logSessionCancelled(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "PATIENT", "No longer needed");

        assertEventContextCleared();
    }

    @Test
    @DisplayName("Should clear MDC after reschedule log")
    void shouldClearMdcAfterRescheduleLog() {
        loggingService.logSessionRescheduled(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "2026-04-15", "2026-04-16", "SYSTEM", 14);

        assertEventContextCleared();
    }

    @Test
    @DisplayName("Should clear MDC after error log")
    void shouldClearMdcAfterErrorLog() {
        loggingService.logBookingError(
                UUID.randomUUID(), UUID.randomUUID(), "IllegalStateException", "boom", "stack");

        assertEventContextCleared();
    }

    @Test
    @DisplayName("Should clear MDC after slot unavailable log")
    void shouldClearMdcAfterSlotUnavailableLog() {
        loggingService.logSlotNotAvailable(UUID.randomUUID(), "2026-04-15", "14:00");

        assertEventContextCleared();
    }

    private static void assertEventContextCleared() {
        assertNull(MDC.get("event"));
        assertNull(MDC.get("actor"));
    }
}
