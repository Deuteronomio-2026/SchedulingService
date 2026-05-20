package edu.arsw.proyecto.SchedulingService.infrastructure.controller;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.dto.RescheduleSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.in.BookSessionUseCase;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.CancelSessionResponse;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.DeleteAllSessionsResponse;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.SessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulingController - Unit Tests")
class SchedulingControllerTest {

    @Mock
    private BookSessionUseCase bookSessionUseCase;

    private SchedulingController controller;

    @BeforeEach
    void setUp() {
        controller = new SchedulingController(bookSessionUseCase);
    }

    @Test
    @DisplayName("Should book session and return created response")
    void shouldBookSessionAndReturnCreatedResponse() {
        BookSessionDTO command = sampleBookCommand();
        Session session = sampleSession(command.patientId(), command.psychologistId(), sampleSlot());
        when(bookSessionUseCase.book(command)).thenReturn(session);

        ResponseEntity<SessionResponse> response = controller.book(command);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSessionResponse(response.getBody(), session);
        verify(bookSessionUseCase).book(command);
    }

    @Test
    @DisplayName("Should cancel session")
    void shouldCancelSession() {
        UUID sessionId = UUID.randomUUID();

        ResponseEntity<CancelSessionResponse> response = controller.cancel(sessionId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Sesión cancelada exitosamente", response.getBody().message());
        verify(bookSessionUseCase).cancel(sessionId);
    }

    @Test
    @DisplayName("Should delete all sessions")
    void shouldDeleteAllSessions() {
        when(bookSessionUseCase.deleteAll()).thenReturn(4);

        ResponseEntity<DeleteAllSessionsResponse> response = controller.deleteAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Sesiones eliminadas exitosamente", response.getBody().message());
        assertEquals(4, response.getBody().deletedCount());
    }

    @Test
    @DisplayName("Should reschedule session")
    void shouldRescheduleSession() {
        UUID sessionId = UUID.randomUUID();
        RescheduleSessionDTO command = new RescheduleSessionDTO(
                LocalDate.of(2026, 4, 16), LocalTime.of(9, 0), LocalTime.of(10, 0));
        Session session = sampleSession(UUID.randomUUID(), UUID.randomUUID(),
                new TimeSlot(command.date(), command.startTime(), command.endTime()));
        when(bookSessionUseCase.reschedule(sessionId, command)).thenReturn(session);

        ResponseEntity<SessionResponse> response = controller.reschedule(sessionId, command);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSessionResponse(response.getBody(), session);
        verify(bookSessionUseCase).reschedule(sessionId, command);
    }

    @Test
    @DisplayName("Should list all sessions")
    void shouldListAllSessions() {
        Session first = sampleSession(UUID.randomUUID(), UUID.randomUUID(), sampleSlot());
        Session second = sampleSession(UUID.randomUUID(), UUID.randomUUID(),
                new TimeSlot(LocalDate.of(2026, 4, 16), LocalTime.of(9, 0), LocalTime.of(10, 0)));
        when(bookSessionUseCase.findAll()).thenReturn(List.of(first, second));

        ResponseEntity<List<SessionResponse>> response = controller.getAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertSessionResponse(response.getBody().get(0), first);
        assertSessionResponse(response.getBody().get(1), second);
    }

    private static BookSessionDTO sampleBookCommand() {
        return new BookSessionDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );
    }

    private static Session sampleSession(UUID patientId, UUID psychologistId, TimeSlot slot) {
        return Session.reconstituteFromPersistence(
                UUID.randomUUID(),
                patientId,
                psychologistId,
                slot,
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );
    }

    private static TimeSlot sampleSlot() {
        return new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
    }

    private static void assertSessionResponse(SessionResponse response, Session session) {
        assertNotNull(response);
        assertEquals(session.getId(), response.getId());
        assertEquals(session.getPatientId(), response.getPatientId());
        assertEquals(session.getPsychologistId(), response.getPsychologistId());
        assertEquals(session.getTimeSlot().getDate(), response.getDate());
        assertEquals(session.getTimeSlot().getStartTime(), response.getStartTime());
        assertEquals(session.getTimeSlot().getEndTime(), response.getEndTime());
        assertEquals(session.getType(), response.getType());
        assertEquals(session.getAttentionType(), response.getAttentionType());
        assertEquals(session.getStatus(), response.getStatus());
        assertEquals(session.getCreatedAt(), response.getCreatedAt());
    }
}
