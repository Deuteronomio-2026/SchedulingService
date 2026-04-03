package edu.arsw.proyecto.SchedulingService.application.service;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.out.EventPublisherPort;
import edu.arsw.proyecto.SchedulingService.application.port.out.SessionRepositoryPort;
import edu.arsw.proyecto.SchedulingService.application.port.out.SlotLockPort;
import edu.arsw.proyecto.SchedulingService.domain.exception.SessionNotFoundException;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import edu.arsw.proyecto.SchedulingService.domain.service.SchedulingDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
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

    private BookSessionApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new BookSessionApplicationService(
                domainService, sessionRepository, slotLock, eventPublisher
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
                SessionType.VIRTUAL
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL);

        when(slotLock.isLocked(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        Session result = applicationService.book(dto);

        assertNotNull(result);
        assertEquals(SessionStatus.CONFIRMED, result.getStatus());

        verify(slotLock).isLocked(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository).existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class));
        verify(domainService).createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL));
        verify(slotLock).lockSlot(eq(psychologistId), any(TimeSlot.class));
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
                SessionType.VIRTUAL
        );

        when(slotLock.isLocked(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.book(dto)
        );

        assertEquals("Horario no disponible", exception.getMessage());

        verify(slotLock).isLocked(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository, never()).existsByPsychologistAndSlot(any(), any());
        verify(domainService, never()).createSession(any(), any(), any(), any());
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
                SessionType.VIRTUAL
        );

        when(slotLock.isLocked(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(true);

        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> applicationService.book(dto)
        );

        assertEquals("Horario ya reservado", exception.getMessage());

        verify(slotLock).isLocked(eq(psychologistId), any(TimeSlot.class));
        verify(sessionRepository).existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class));
        verify(domainService, never()).createSession(any(), any(), any(), any());
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
                SessionType.VIRTUAL
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL);

        when(slotLock.isLocked(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        applicationService.book(dto);

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        Session savedSession = sessionCaptor.getValue();
        assertEquals(SessionStatus.CONFIRMED, savedSession.getStatus());
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
        Session existingSession = new Session(patientId, psychologistId, timeSlot, SessionType.VIRTUAL);
        existingSession.confirm();

        Session reconstitutedSession = Session.reconstituteFromPersistence(
                sessionId, patientId, psychologistId, timeSlot,
                SessionType.VIRTUAL, SessionStatus.CONFIRMED, java.time.LocalDateTime.now()
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
                SessionType.VIRTUAL
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL);

        when(slotLock.isLocked(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL)))
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
                SessionType.VIRTUAL
        );

        TimeSlot expectedSlot = new TimeSlot(dto.date(), dto.startTime(), dto.endTime());
        Session mockSession = new Session(patientId, psychologistId, expectedSlot, SessionType.VIRTUAL);

        when(slotLock.isLocked(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(sessionRepository.existsByPsychologistAndSlot(eq(psychologistId), any(TimeSlot.class))).thenReturn(false);
        when(domainService.createSession(eq(patientId), eq(psychologistId), any(TimeSlot.class), eq(SessionType.VIRTUAL)))
                .thenReturn(mockSession);
        when(sessionRepository.save(any(Session.class))).thenReturn(mockSession);

        applicationService.book(dto);

        verify(slotLock).lockSlot(eq(psychologistId), any(TimeSlot.class));
    }
}
