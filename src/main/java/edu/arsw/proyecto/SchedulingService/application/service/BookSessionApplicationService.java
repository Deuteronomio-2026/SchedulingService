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
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import edu.arsw.proyecto.SchedulingService.domain.service.SchedulingDomainService;
import edu.arsw.proyecto.SchedulingService.infrastructure.observability.ObservabilityFacade;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookSessionApplicationService implements BookSessionUseCase {

    private final SchedulingDomainService domainService;
    private final SessionRepositoryPort sessionRepository;
    private final SlotLockPort slotLock;
    private final EventPublisherPort eventPublisher;
    private final ObservabilityFacade observability;

    public BookSessionApplicationService(
            SchedulingDomainService domainService,
            SessionRepositoryPort sessionRepository,
            SlotLockPort slotLock,
            EventPublisherPort eventPublisher,
            ObservabilityFacade observability) {
        this.domainService = domainService;
        this.sessionRepository = sessionRepository;
        this.slotLock = slotLock;
        this.eventPublisher = eventPublisher;
        this.observability = observability;
    }

    @Override
    public Session book(BookSessionDTO cmd) {
        Timer.Sample sample = observability.startLatencyMeasurement();
        observability.recordBookingAttempt();

        try {
            TimeSlot slot = new TimeSlot(
                    cmd.date(), cmd.startTime(), cmd.endTime()
            );

            if (slotLock.isLocked(cmd.psychologistId(), slot)) {
                observability.recordDoubleBookingDetected(
                        cmd.psychologistId(), null, null, cmd.patientId(), 0);
                throw new SlotNotAvailableException("Horario no disponible");
            }

            if (sessionRepository.existsByPsychologistAndSlot(
                    cmd.psychologistId(), slot)) {
                observability.recordRejectedBooking(
                        cmd.psychologistId(),
                        cmd.date().toString(),
                        cmd.startTime().toString());
                throw new SlotNotAvailableException("Horario ya reservado");
            }

            Session session = domainService.createSession(
                    cmd.patientId(), cmd.psychologistId(), slot, cmd.type(), cmd.attentionType()
            );

            session.confirm();

            slotLock.lockSlot(cmd.psychologistId(), slot);

            Session saved = sessionRepository.save(session);

            long latencyMs = observability.stopLatencyMeasurement(sample);
            observability.recordSuccessfulBooking(
                    saved.getId(),
                    cmd.patientId(),
                    cmd.psychologistId(),
                    cmd.type().toString(),
                    latencyMs);

            eventPublisher.publishSessionBooked(saved);

            return saved;
        } catch (SlotNotAvailableException | SessionNotFoundException e) {
            observability.stopLatencyMeasurement(sample);
            throw e;
        } catch (Exception e) {
            long latencyMs = observability.stopLatencyMeasurement(sample);
            observability.recordBookingError(
                    cmd.patientId(),
                    cmd.psychologistId(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    getStackTrace(e));
            throw e;
        }
    }

    @Override
    public Session reschedule(UUID sessionId, RescheduleSessionDTO cmd) {
        Timer.Sample sample = observability.startLatencyMeasurement();

        try {
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

            long latencyMs = observability.stopLatencyMeasurement(sample);
            observability.recordSessionReschedule(
                    saved.getId(),
                    saved.getPatientId(),
                    saved.getPsychologistId(),
                    oldSlot.getDate().toString(),
                    newSlot.getDate().toString(),
                    "SYSTEM",
                    latencyMs);

            eventPublisher.publishSessionRescheduled(saved);
            return saved;
        } catch (Exception e) {
            observability.stopLatencyMeasurement(sample);
            throw e;
        }
    }

    @Override
    public void cancel(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Sesión no encontrada"));

        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new SessionNotFoundException("No hay una sesión activa con el ID indicado");
        }

        session.cancel();
        sessionRepository.save(session);
        slotLock.unlockSlot(session.getPsychologistId(), session.getTimeSlot());

        observability.recordSessionCancellation(
                session.getId(),
                session.getPatientId(),
                session.getPsychologistId(),
                "SYSTEM",
                "Manual cancellation");

        eventPublisher.publishSessionCancelled(session);
    }

    @Override
    public int deleteAll() {
        List<Session> sessions = sessionRepository.findAll();
        for (Session session : sessions) {
            slotLock.unlockSlot(session.getPsychologistId(), session.getTimeSlot());
        }
        sessionRepository.deleteAll();
        return sessions.size();
    }

    @Override
    public List<Session> findAll() {
        return sessionRepository.findAll();
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}