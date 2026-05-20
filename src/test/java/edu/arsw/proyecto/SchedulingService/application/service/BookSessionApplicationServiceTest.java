package edu.arsw.proyecto.SchedulingService.application.service;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.dto.RescheduleSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.out.EventPublisherPort;
import edu.arsw.proyecto.SchedulingService.application.port.out.SessionRepositoryPort;
import edu.arsw.proyecto.SchedulingService.application.port.out.SlotLockPort;
import edu.arsw.proyecto.SchedulingService.domain.exception.SessionNotFoundException;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import edu.arsw.proyecto.SchedulingService.domain.service.SchedulingDomainService;
import edu.arsw.proyecto.SchedulingService.infrastructure.observability.ObservabilityFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookSessionApplicationService - Unit Tests")
class BookSessionApplicationServiceTest {

    @Mock
    private SchedulingDomainService domainService;

    @Mock
    private SessionRepositoryPort sessionRepository;

    @Mock
    private SlotLockPort slotLock;

    @Mock
    private EventPublisherPort eventPublisher;

    @Mock
    private ObservabilityFacade observability;

    private BookSessionApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new BookSessionApplicationService(
                domainService, sessionRepository, slotLock, eventPublisher, observability
        );
    }

    @Test
    @DisplayName("Should book session successfully when slot is available")
    void shouldBookSessionSuccessfullyWhenSlotIsAvailable() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ);

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        Session result = applicationService.book(dto);

        assertNotNull(result);
        assertEquals(SessionStatus.CONFIRMED, result.getStatus());

        verify(slotLock).tryLockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository).existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class));
        verify(domainService).createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ));
        verify(slotLock).tryLockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository).save(any(Session.class));
        verify(eventPublisher).publishSessionBooked(any(Session.class));
    }

    @Test
    @DisplayName("Should throw SlotNotAvailableException when slot is locked")
    void shouldThrowSlotNotAvailableExceptionWhenSlotIsLocked() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.book(dto)
        );

        assertEquals("Horario no disponible", exception.getMessage());

        verify(slotLock).tryLockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository, never()).existsByPsychologistAndSlot(any(), any());
        verify(domainService, never()).createSession(any(), any(), any(), any(), any());
        verify(sessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishSessionBooked(any());
    }

    @Test
    @DisplayName("Should throw SlotNotAvailableException when slot is already booked")
    void shouldThrowSlotNotAvailableExceptionWhenSlotIsAlreadyBooked() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.book(dto)
        );

        assertEquals("Horario ya reservado", exception.getMessage());

        verify(slotLock).tryLockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository).existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class));
        verify(domainService, never()).createSession(any(), any(), any(), any(), any());
        verify(sessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishSessionBooked(any());
    }

    @Test
    @DisplayName("Should confirm session before saving")
    void shouldConfirmSessionBeforeSaving() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ);

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        applicationService.book(dto);

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        Session savedSession = sessionCaptor.getValue();
        assertEquals(SessionStatus.CONFIRMED, savedSession.getStatus());
    }

    @Test
    @DisplayName("Should release slot and reject booking when save violates unique constraint")
    void shouldReleaseSlotAndRejectBookingWhenSaveViolatesUniqueConstraint() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );
        TimeSlot slot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session session = new Session(patientId, psychologistId, slot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ);

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class),
                eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ))).thenReturn(session);
        when(sessionRepository.save(any(Session.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.book(dto)
        );

        assertEquals("Horario ya reservado", exception.getMessage());
        verify(slotLock).unlockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(observability).recordRejectedBooking(
                psychologistId, dto.date().toString(), dto.startTime().toString());
    }

    @Test
    @DisplayName("Should release slot and record unexpected booking errors")
    void shouldReleaseSlotAndRecordUnexpectedBookingErrors() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class),
                eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ)))
                .thenThrow(new IllegalStateException("boom"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> applicationService.book(dto)
        );

        assertEquals("boom", exception.getMessage());
        verify(slotLock).unlockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(observability).recordBookingError(
                eq(patientId),
                eq(psychologistId),
                eq("IllegalStateException"),
                eq("boom"),
                anyString());
    }

    @Test
    @DisplayName("Should cancel session successfully when session exists")
    void shouldCancelSessionSuccessfullyWhenSessionExists() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
        Session existingSession = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ);
        existingSession.confirm();

        Session reconstitutedSession = Session.reconstituteFromPersistence(
                sessionId, patientId, psychologistId, timeSlot,
                SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ, SessionStatus.CONFIRMED, java.time.LocalDateTime.now()
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(reconstitutedSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(reconstitutedSession);

        applicationService.cancel(sessionId);

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        Session cancelledSession = sessionCaptor.getValue();
        assertEquals(SessionStatus.CANCELLED, cancelledSession.getStatus());

        verify(slotLock).unlockSlot(psychologistId, timeSlot);
        verify(eventPublisher).publishSessionCancelled(any(Session.class));
    }

    @Test
    @DisplayName("Should throw SessionNotFoundException when session does not exist")
    void shouldThrowSessionNotFoundExceptionWhenSessionDoesNotExist() {
        UUID sessionId = UUID.randomUUID();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        SessionNotFoundException exception = assertThrows(
                SessionNotFoundException.class,
                () -> applicationService.cancel(sessionId)
        );

        assertEquals("Sesión no encontrada", exception.getMessage());

        verify(sessionRepository).findById(sessionId);
        verify(sessionRepository, never()).save(any());
        verify(slotLock, never()).unlockSlot(any(), any());
        verify(eventPublisher, never()).publishSessionCancelled(any());
    }

    @Test
    @DisplayName("Should throw SessionNotFoundException when session is already cancelled")
    void shouldThrowSessionNotFoundExceptionWhenSessionIsAlreadyCancelled() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();

        TimeSlot timeSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session cancelledSession = Session.reconstituteFromPersistence(
                sessionId,
                patientId,
                psychologistId,
                timeSlot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CANCELLED,
                LocalDateTime.now()
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(cancelledSession));

        SessionNotFoundException exception = assertThrows(
                SessionNotFoundException.class,
                () -> applicationService.cancel(sessionId)
        );

        assertEquals("No hay una sesión activa con el ID indicado", exception.getMessage());
        verify(sessionRepository, never()).save(any());
        verify(slotLock, never()).unlockSlot(any(), any());
        verify(eventPublisher, never()).publishSessionCancelled(any());
    }

    @Test
    @DisplayName("Should publish event after successful booking")
    void shouldPublishEventAfterSuccessfulBooking() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ);

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        applicationService.book(dto);

        verify(eventPublisher).publishSessionBooked(any(Session.class));
    }

    @Test
    @DisplayName("Should lock slot after creating session")
    void shouldLockSlotAfterCreatingSession() {
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        BookSessionDTO dto = new BookSessionDTO(
                patientId,
                psychologistId,
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ);

        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL), eq(SessionAttentionType.PRIMERA_VEZ)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        applicationService.book(dto);

        verify(slotLock).tryLockSlot(eq(psychologistId), any(TimeSlot.class));
    }

    @Test
    @DisplayName("Should reschedule session successfully when new slot is available")
    void shouldRescheduleSessionSuccessfullyWhenNewSlotIsAvailable() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();

        TimeSlot oldSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = Session.reconstituteFromPersistence(
                sessionId,
                patientId,
                psychologistId,
                oldSlot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.now()
        );

        RescheduleSessionDTO dto = new RescheduleSessionDTO(
                LocalDate.of(2026, 4, 16),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        Session result = applicationService.reschedule(sessionId, dto);

        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 4, 16), result.getTimeSlot().getDate());
        assertEquals(LocalTime.of(9, 0), result.getTimeSlot().getStartTime());
        assertEquals(LocalTime.of(10, 0), result.getTimeSlot().getEndTime());

        verify(slotLock).tryLockSlot(eq(psychologistId), any(TimeSlot.class));
        verify(slotLock).unlockSlot(eq(psychologistId), eq(oldSlot));
        verify(sessionRepository).save(session);
        verify(eventPublisher).publishSessionRescheduled(session);
    }

    @Test
    @DisplayName("Should reject reschedule to the same slot")
    void shouldRejectRescheduleToTheSameSlot() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot oldSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
        Session session = Session.reconstituteFromPersistence(
                sessionId,
                patientId,
                psychologistId,
                oldSlot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.now()
        );
        RescheduleSessionDTO dto = new RescheduleSessionDTO(
                oldSlot.getDate(), oldSlot.getStartTime(), oldSlot.getEndTime());
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> applicationService.reschedule(sessionId, dto)
        );

        assertEquals("La nueva franja horaria debe ser diferente a la actual", exception.getMessage());
        verify(slotLock, never()).tryLockSlot(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject reschedule when new slot already exists")
    void shouldRejectRescheduleWhenNewSlotAlreadyExists() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot oldSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
        Session session = Session.reconstituteFromPersistence(
                sessionId,
                patientId,
                psychologistId,
                oldSlot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.now()
        );
        RescheduleSessionDTO dto = new RescheduleSessionDTO(
                LocalDate.of(2026, 4, 16),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.reschedule(sessionId, dto)
        );

        assertEquals("Horario ya reservado", exception.getMessage());
        verify(slotLock).unlockSlot(eq(psychologistId), argThat(slot ->
                slot.getDate().equals(dto.date())
                        && slot.getStartTime().equals(dto.startTime())
                        && slot.getEndTime().equals(dto.endTime())));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should unlock new slot when reschedule save violates unique constraint")
    void shouldUnlockNewSlotWhenRescheduleSaveViolatesUniqueConstraint() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();
        TimeSlot oldSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
        Session session = Session.reconstituteFromPersistence(
                sessionId,
                patientId,
                psychologistId,
                oldSlot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.now()
        );
        RescheduleSessionDTO dto = new RescheduleSessionDTO(
                LocalDate.of(2026, 4, 16),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.save(session)).thenThrow(new DataIntegrityViolationException("duplicate"));

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.reschedule(sessionId, dto)
        );

        assertEquals("Horario ya reservado", exception.getMessage());
        verify(slotLock).unlockSlot(psychologistId, oldSlot);
        verify(slotLock).unlockSlot(eq(psychologistId), argThat(slot ->
                slot.getDate().equals(dto.date())
                        && slot.getStartTime().equals(dto.startTime())
                        && slot.getEndTime().equals(dto.endTime())));
    }

    @Test
    @DisplayName("Should throw SlotNotAvailableException when new slot is locked")
    void shouldThrowSlotNotAvailableExceptionWhenNewSlotIsLocked() {
        UUID sessionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID psychologistId = UUID.randomUUID();

        TimeSlot oldSlot = new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        Session session = Session.reconstituteFromPersistence(
                sessionId,
                patientId,
                psychologistId,
                oldSlot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.now()
        );

        RescheduleSessionDTO dto = new RescheduleSessionDTO(
                LocalDate.of(2026, 4, 16),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(slotLock.tryLockSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.reschedule(sessionId, dto)
        );

        assertEquals("Horario no disponible", exception.getMessage());
        verify(sessionRepository, never()).save(any());
        verify(eventPublisher, never()).publishSessionRescheduled(any());
    }

    @Test
    @DisplayName("Should throw SessionNotFoundException when rescheduling unknown session")
    void shouldThrowSessionNotFoundExceptionWhenReschedulingUnknownSession() {
        UUID sessionId = UUID.randomUUID();

        RescheduleSessionDTO dto = new RescheduleSessionDTO(
                LocalDate.of(2026, 4, 16),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        SessionNotFoundException exception = assertThrows(
                SessionNotFoundException.class,
                () -> applicationService.reschedule(sessionId, dto)
        );

        assertEquals("Sesión no encontrada", exception.getMessage());
        verify(slotLock, never()).tryLockSlot(any(), any());
        verify(eventPublisher, never()).publishSessionRescheduled(any());
    }

        @Test
        @DisplayName("Should delete all sessions and unlock all slots")
        void shouldDeleteAllSessionsAndUnlockAllSlots() {
                UUID patientId1 = UUID.randomUUID();
                UUID psychologistId1 = UUID.randomUUID();
                TimeSlot slot1 = new TimeSlot(LocalDate.of(2026, 4, 15), LocalTime.of(14, 0), LocalTime.of(15, 0));
                Session session1 = Session.reconstituteFromPersistence(
                                UUID.randomUUID(), patientId1, psychologistId1, slot1,
                                SessionType.VIRTUAL, SessionAttentionType.PRIMERA_VEZ, SessionStatus.CONFIRMED, LocalDateTime.now()
                );

                UUID patientId2 = UUID.randomUUID();
                UUID psychologistId2 = UUID.randomUUID();
                TimeSlot slot2 = new TimeSlot(LocalDate.of(2026, 4, 16), LocalTime.of(9, 0), LocalTime.of(10, 0));
                Session session2 = Session.reconstituteFromPersistence(
                                UUID.randomUUID(), patientId2, psychologistId2, slot2,
                        SessionType.PRESENTIAL, SessionAttentionType.SEGUIMIENTO, SessionStatus.CONFIRMED, LocalDateTime.now()
                );

                when(sessionRepository.findAll()).thenReturn(List.of(session1, session2));

                int deletedCount = applicationService.deleteAll();

                assertEquals(2, deletedCount);
                verify(slotLock).unlockSlot(psychologistId1, slot1);
                verify(slotLock).unlockSlot(psychologistId2, slot2);
                verify(sessionRepository).deleteAll();
                verifyNoInteractions(domainService, eventPublisher);
        }

        @Test
        @DisplayName("Should return all sessions")
        void shouldReturnAllSessions() {
                Session session = Session.reconstituteFromPersistence(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                new TimeSlot(LocalDate.of(2026, 4, 15), LocalTime.of(14, 0), LocalTime.of(15, 0)),
                                SessionType.VIRTUAL,
                                SessionAttentionType.PRIMERA_VEZ,
                                SessionStatus.CONFIRMED,
                                LocalDateTime.now()
                );
                when(sessionRepository.findAll()).thenReturn(List.of(session));

                List<Session> sessions = applicationService.findAll();

                assertEquals(List.of(session), sessions);
                verifyNoInteractions(domainService, slotLock, eventPublisher);
        }
}
