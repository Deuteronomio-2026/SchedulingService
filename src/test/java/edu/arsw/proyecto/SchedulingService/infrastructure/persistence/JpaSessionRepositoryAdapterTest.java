package edu.arsw.proyecto.SchedulingService.infrastructure.persistence;

import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSessionRepositoryAdapter - Unit Tests")
class JpaSessionRepositoryAdapterTest {

    @Mock
    private JpaSessionSpringRepository jpa;

    private JpaSessionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaSessionRepositoryAdapter(jpa);
    }

    @Test
    @DisplayName("Should save domain session as JPA entity")
    void shouldSaveDomainSessionAsJpaEntity() {
        Session session = sampleSession();

        adapter.save(session);

        ArgumentCaptor<SessionJpaEntity> captor = ArgumentCaptor.forClass(SessionJpaEntity.class);
        verify(jpa).save(captor.capture());
        SessionJpaEntity entity = captor.getValue();
        assertEquals(session.getId(), entity.getId());
        assertEquals(session.getPatientId(), entity.getPatientId());
        assertEquals(session.getPsychologistId(), entity.getPsychologistId());
        assertEquals(session.getTimeSlot().getDate(), entity.getDate());
        assertEquals(session.getTimeSlot().getStartTime(), entity.getStartTime());
        assertEquals(session.getTimeSlot().getEndTime(), entity.getEndTime());
        assertEquals(session.getType(), entity.getType());
        assertEquals(session.getAttentionType(), entity.getAttentionType());
        assertEquals(session.getStatus(), entity.getStatus());
        assertEquals(session.getCreatedAt(), entity.getCreatedAt());
    }

    @Test
    @DisplayName("Should find session by id")
    void shouldFindSessionById() {
        UUID id = UUID.randomUUID();
        SessionJpaEntity entity = sampleEntity(id);
        when(jpa.findById(id)).thenReturn(Optional.of(entity));

        Optional<Session> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(entity.getPatientId(), result.get().getPatientId());
        assertEquals(entity.getPsychologistId(), result.get().getPsychologistId());
        assertEquals(entity.getDate(), result.get().getTimeSlot().getDate());
        assertEquals(entity.getStartTime(), result.get().getTimeSlot().getStartTime());
        assertEquals(entity.getEndTime(), result.get().getTimeSlot().getEndTime());
        assertEquals(entity.getType(), result.get().getType());
        assertEquals(entity.getAttentionType(), result.get().getAttentionType());
        assertEquals(entity.getStatus(), result.get().getStatus());
        assertEquals(entity.getCreatedAt(), result.get().getCreatedAt());
    }

    @Test
    @DisplayName("Should return empty when session does not exist")
    void shouldReturnEmptyWhenSessionDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(jpa.findById(id)).thenReturn(Optional.empty());

        assertTrue(adapter.findById(id).isEmpty());
    }

    @Test
    @DisplayName("Should check existing psychologist slot")
    void shouldCheckExistingPsychologistSlot() {
        UUID psychologistId = UUID.randomUUID();
        TimeSlot slot = sampleSlot();
        when(jpa.existsByPsychologistIdAndDateAndStartTime(
                psychologistId, slot.getDate(), slot.getStartTime())).thenReturn(true);

        assertTrue(adapter.existsByPsychologistAndSlot(psychologistId, slot));
    }

    @Test
    @DisplayName("Should delete all in batch")
    void shouldDeleteAllInBatch() {
        adapter.deleteAll();

        verify(jpa).deleteAllInBatch();
    }

    @Test
    @DisplayName("Should map all JPA entities to domain sessions")
    void shouldMapAllJpaEntitiesToDomainSessions() {
        SessionJpaEntity first = sampleEntity(UUID.randomUUID());
        SessionJpaEntity second = sampleEntity(UUID.randomUUID());
        when(jpa.findAll()).thenReturn(List.of(first, second));

        List<Session> sessions = adapter.findAll();

        assertEquals(2, sessions.size());
        assertEquals(first.getId(), sessions.get(0).getId());
        assertEquals(second.getId(), sessions.get(1).getId());
        assertFalse(sessions.get(0).getId().equals(sessions.get(1).getId()));
    }

    private static Session sampleSession() {
        return Session.reconstituteFromPersistence(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                sampleSlot(),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );
    }

    private static SessionJpaEntity sampleEntity(UUID id) {
        SessionJpaEntity entity = new SessionJpaEntity();
        entity.setId(id);
        entity.setPatientId(UUID.randomUUID());
        entity.setPsychologistId(UUID.randomUUID());
        entity.setDate(LocalDate.of(2026, 4, 15));
        entity.setStartTime(LocalTime.of(14, 0));
        entity.setEndTime(LocalTime.of(15, 0));
        entity.setType(SessionType.PRESENTIAL);
        entity.setAttentionType(SessionAttentionType.SEGUIMIENTO);
        entity.setStatus(SessionStatus.CONFIRMED);
        entity.setCreatedAt(LocalDateTime.of(2026, 4, 1, 9, 30));
        return entity;
    }

    private static TimeSlot sampleSlot() {
        return new TimeSlot(
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );
    }
}
