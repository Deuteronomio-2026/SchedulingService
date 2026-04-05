package edu.arsw.proyecto.SchedulingService.domain.service;

import edu.arsw.proyecto.SchedulingService.application.port.out.DomainLockPort;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulingDomainService - Unit Tests")
class SchedulingDomainServiceTest {

    @Mock
    private DomainLockPort lockPort;

    private SchedulingDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new SchedulingDomainService(lockPort);
    }

    @Test
    @DisplayName("Should create session successfully when lock is acquired")
    void shouldCreateSessionSuccessfullyWhenLockIsAcquired() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        when(lockPort.acquireLock(psychologistId)).thenReturn(true);

        Session session = domainService.createSession(
                patientId, psychologistId, timeSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ
        );

        assertNotNull(session);
        assertEquals(patientId, session.getPatientId());
        assertEquals(psychologistId, session.getPsychologistId());
        assertEquals(timeSlot, session.getTimeSlot());
        assertEquals(SessionType.VIRTUAL, session.getType());
        assertEquals(SessionAttentionType.PRIMERA_VEZ, session.getAttentionType());

        verify(lockPort).acquireLock(psychologistId);
        verify(lockPort).releaseLock(psychologistId);
    }

    @Test
    @DisplayName("Should throw SlotNotAvailableException when lock cannot be acquired")
    void shouldThrowSlotNotAvailableExceptionWhenLockCannotBeAcquired() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        when(lockPort.acquireLock(psychologistId)).thenReturn(false);

        assertThrows(SlotNotAvailableException.class, () ->
                domainService.createSession(patientId, psychologistId, timeSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ)
        );

        verify(lockPort).acquireLock(psychologistId);
        verify(lockPort, never()).releaseLock(psychologistId);
    }

    @Test
    @DisplayName("Should release lock even if session creation fails")
    void shouldReleaseLockEvenIfSessionCreationFails() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        when(lockPort.acquireLock(psychologistId)).thenReturn(true);

        Session session = domainService.createSession(
                patientId, psychologistId, timeSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ
        );

        assertNotNull(session);
        verify(lockPort).releaseLock(psychologistId);
    }

    @Test
    @DisplayName("Should create PRESENTIAL session type")
    void shouldCreatePresentialSessionType() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        when(lockPort.acquireLock(psychologistId)).thenReturn(true);

        Session session = domainService.createSession(
                patientId, psychologistId, timeSlot, SessionType.PRESENTIAL, SessionAttentionType.SEGUIMIENTO
        );

        assertEquals(SessionType.PRESENTIAL, session.getType());
        assertEquals(SessionAttentionType.SEGUIMIENTO, session.getAttentionType());
        verify(lockPort).releaseLock(psychologistId);
    }

    @Test
    @DisplayName("Should handle multiple psychologists independently")
    void shouldHandleMultiplePsychologistsIndependently() {
        UUID patientId = UUID.randomUUID();
        UUID psychologist1 = UUID.randomUUID();
        UUID psychologist2 = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        when(lockPort.acquireLock(psychologist1)).thenReturn(true);
        when(lockPort.acquireLock(psychologist2)).thenReturn(true);

        Session session1 = domainService.createSession(
                patientId, psychologist1, timeSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ
        );
        Session session2 = domainService.createSession(
                patientId, psychologist2, timeSlot, SessionType.VIRTUAL, SessionAttentionType.SEGUIMIENTO
        );

        assertNotNull(session1);
        assertNotNull(session2);
        assertEquals(psychologist1, session1.getPsychologistId());
        assertEquals(psychologist2, session2.getPsychologistId());

        verify(lockPort).acquireLock(psychologist1);
        verify(lockPort).acquireLock(psychologist2);
        verify(lockPort).releaseLock(psychologist1);
        verify(lockPort).releaseLock(psychologist2);
    }
}
