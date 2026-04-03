package edu.arsw.proyecto.SchedulingService.application.service;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.dto.RescheduleSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.in.BookSessionUseCase;
import edu.arsw.proyecto.SchedulingService.application.port.out.EventPublisherPort;
import edu.arsw.proyecto.SchedulingService.application.port.out.SessionRepositoryPort;
import edu.arsw.proyecto.SchedulingService.application.port.out.SlotLockPort;
import edu.arsw.proyecto.SchedulingService.domain.exception.SessionNotFoundException;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import edu.arsw.proyecto.SchedulingService.domain.service.SchedulingDomainService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookSessionApplicationService implements BookSessionUseCase {

    private final SchedulingDomainService domainService;
    private final SessionRepositoryPort sessionRepository;
    private final SlotLockPort slotLock;
    private final EventPublisherPort eventPublisher;

    public BookSessionApplicationService(
            SchedulingDomainService domainService,
            SessionRepositoryPort sessionRepository,
            SlotLockPort slotLock,
            EventPublisherPort eventPublisher) {
        this.domainService = domainService;
        this.sessionRepository = sessionRepository;
        this.slotLock = slotLock;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Session book(BookSessionDTO cmd) {
        TimeSlot slot = new TimeSlot(
                cmd.date(), cmd.startTime(), cmd.endTime()
        );

        if (slotLock.isLocked(cmd.psychologistId(), slot)) {
            throw new SlotNotAvailableException("Horario no disponible");
        }

        if (sessionRepository.existsByPsychologistAndSlot(
                cmd.psychologistId(), slot)) {
            throw new SlotNotAvailableException("Horario ya reservado");
        }

        Session session = domainService.createSession(
                cmd.patientId(), cmd.psychologistId(), slot, cmd.type()
        );

        session.confirm();

        slotLock.lockSlot(cmd.psychologistId(), slot);

        Session saved = sessionRepository.save(session);

        eventPublisher.publishSessionBooked(saved);

        return saved;
    }

    @Override
    public Session reschedule(UUID sessionId, RescheduleSessionDTO cmd) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));

        TimeSlot oldSlot = session.getTimeSlot();
        TimeSlot newSlot = new TimeSlot(cmd.date(), cmd.startTime(), cmd.endTime());

        boolean sameSlot = oldSlot.getDate().equals(newSlot.getDate())
                && oldSlot.getStartTime().equals(newSlot.getStartTime())
                && oldSlot.getEndTime().equals(newSlot.getEndTime());

        if (sameSlot) {
            throw new IllegalArgumentException("La nueva franja horaria debe ser diferente a la actual");
        }

        if (slotLock.isLocked(session.getPsychologistId(), newSlot)) {
            throw new SlotNotAvailableException("Horario no disponible");
        }

        if (sessionRepository.existsByPsychologistAndSlot(session.getPsychologistId(), newSlot)) {
            throw new SlotNotAvailableException("Horario ya reservado");
        }

        slotLock.lockSlot(session.getPsychologistId(), newSlot);
        slotLock.unlockSlot(session.getPsychologistId(), oldSlot);

        session.reschedule(newSlot);
        Session saved = sessionRepository.save(session);
        eventPublisher.publishSessionRescheduled(saved);
        return saved;
    }

    @Override
    public void cancel(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));

        session.cancel();
        sessionRepository.save(session);
        slotLock.unlockSlot(session.getPsychologistId(), session.getTimeSlot());
        eventPublisher.publishSessionCancelled(session);
    }

    @Override
    public List<Session> findAll() {
        return sessionRepository.findAll();
    }
}