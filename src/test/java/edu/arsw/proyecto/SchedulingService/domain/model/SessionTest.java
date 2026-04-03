package edu.arsw.proyecto.SchedulingService.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Session - Unit Tests")
class SessionTest {

    @Test
    @DisplayName("Should create new Session with PENDING status")
    void shouldCreateNewSessionWithPendingStatus() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL);

        assertNotNull(session.getId());
        assertEquals(patientId, session.getPatientId());
        assertEquals(psychologistId, session.getPsychologistId());
        assertEquals(timeSlot, session.getTimeSlot());
        assertEquals(SessionType.VIRTUAL, session.getType());
        assertEquals(SessionStatus.PENDING, session.getStatus());
        assertNotNull(session.getCreatedAt());
    }

    @Test
    @DisplayName("Should confirm session and change status to CONFIRMED")
    void shouldConfirmSessionAndChangeStatusToConfirmed() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL);
        assertEquals(SessionStatus.PENDING, session.getStatus());

        session.confirm();

        assertEquals(SessionStatus.CONFIRMED, session.getStatus());
    }

    @Test
    @DisplayName("Should cancel session and change status to CANCELLED")
    void shouldCancelSessionAndChangeStatusToCancelled() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL);
        session.confirm();

        session.cancel();

        assertEquals(SessionStatus.CANCELLED, session.getStatus());
    }

    @Test
    @DisplayName("Should complete session and change status to COMPLETED")
    void shouldCompleteSessionAndChangeStatusToCompleted() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL);
        session.confirm();

        session.complete();

        assertEquals(SessionStatus.COMPLETED, session.getStatus());
    }

    @Test
    @DisplayName("Should reconstitute session from persistence with all data")
    void shouldReconstituteSessionFromPersistenceWithAllData() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
        java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

        Session session = Session.reconstituteFromPersistence(
                id, patientId, psychologistId, timeSlot,
                SessionType.PRESENTIAL, SessionStatus.CONFIRMED, createdAt
        );

        assertEquals(id, session.getId());
        assertEquals(patientId, session.getPatientId());
        assertEquals(psychologistId, session.getPsychologistId());
        assertEquals(timeSlot, session.getTimeSlot());
        assertEquals(SessionType.PRESENTIAL, session.getType());
        assertEquals(SessionStatus.CONFIRMED, session.getStatus());
        assertEquals(createdAt, session.getCreatedAt());
    }

    @Test
    @DisplayName("Should create session with PRESENTIAL type")
    void shouldCreateSessionWithPresentialType() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = new Session(patientId, psychologistId, timeSlot, SessionType.PRESENTIAL);

        assertEquals(SessionType.PRESENTIAL, session.getType());
    }

    @Test
    @DisplayName("Should maintain immutability of core attributes")
    void shouldMaintainImmutabilityOfCoreAttributes() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL);

        UUID originalId = session.getId();
        UUID originalPatientId = session.getPatientId();
        UUID originalPsychologistId = session.getPsychologistId();

        session.confirm();
        session.cancel();

        assertEquals(originalId, session.getId());
        assertEquals(originalPatientId, session.getPatientId());
        assertEquals(originalPsychologistId, session.getPsychologistId());
    }

        @Test
        @DisplayName("Should reschedule confirmed session and update timeslot")
        void shouldRescheduleConfirmedSessionAndUpdateTimeslot() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot originalTimeSlot = new TimeSlot(
            LocalDate.of(2026, 4, 15),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0)
        );
        TimeSlot newTimeSlot = new TimeSlot(
            LocalDate.of(2026, 4, 16),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );

        Session session = new Session(patientId, psychologistId, originalTimeSlot, SessionType.VIRTUAL);
        session.confirm();

        session.reschedule(newTimeSlot);

        assertEquals(newTimeSlot, session.getTimeSlot());
        assertEquals(SessionStatus.CONFIRMED, session.getStatus());
        }

        @Test
        @DisplayName("Should throw when trying to reschedule cancelled session")
        void shouldThrowWhenTryingToRescheduleCancelledSession() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot originalTimeSlot = new TimeSlot(
            LocalDate.of(2026, 4, 15),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0)
        );
        TimeSlot newTimeSlot = new TimeSlot(
            LocalDate.of(2026, 4, 16),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );

        Session session = new Session(patientId, psychologistId, originalTimeSlot, SessionType.VIRTUAL);
        session.confirm();
        session.cancel();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> session.reschedule(newTimeSlot)
        );

        assertEquals("No se puede reprogramar una sesión cancelada o completada", exception.getMessage());
        }
}
