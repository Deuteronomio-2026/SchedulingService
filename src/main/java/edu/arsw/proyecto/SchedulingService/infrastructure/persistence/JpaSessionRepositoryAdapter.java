package edu.arsw.proyecto.SchedulingService.infrastructure.persistence;

import edu.arsw.proyecto.SchedulingService.application.port.out.SessionRepositoryPort;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaSessionRepositoryAdapter implements SessionRepositoryPort {

    private final JpaSessionSpringRepository jpa;

    public JpaSessionRepositoryAdapter(JpaSessionSpringRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Session save(Session session) {
        SessionJpaEntity entity = toEntity(session);
        jpa.save(entity);
        return session;
    }

    @Override
    public Optional<Session> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByPsychologistAndSlot(
            UUID psychologistId, TimeSlot slot) {
        return jpa.existsByPsychologistIdAndDateAndStartTime(
                psychologistId, slot.getDate(), slot.getStartTime()
        );
    }

    @Override
    public void deleteAll() {
        jpa.deleteAllInBatch();
    }

    @Override
    public List<Session> findAll() {
        return jpa.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private SessionJpaEntity toEntity(Session s) {
        SessionJpaEntity e = new SessionJpaEntity();
        e.setId(s.getId());
        e.setPatientId(s.getPatientId());
        e.setPsychologistId(s.getPsychologistId());
        e.setDate(s.getTimeSlot().getDate());
        e.setStartTime(s.getTimeSlot().getStartTime());
        e.setEndTime(s.getTimeSlot().getEndTime());
        e.setType(s.getType());
        e.setAttentionType(s.getAttentionType());
        e.setStatus(s.getStatus());
        e.setCreatedAt(s.getCreatedAt());
        return e;
    }

    private Session toDomain(SessionJpaEntity e) {
        TimeSlot slot = new TimeSlot(
                e.getDate(), e.getStartTime(), e.getEndTime()
        );
        return Session.reconstituteFromPersistence(
                e.getId(),
                e.getPatientId(),
                e.getPsychologistId(),
                slot,
                e.getType(),
                e.getAttentionType(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }
}