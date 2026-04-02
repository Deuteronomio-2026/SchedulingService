package edu.arsw.proyecto.SchedulingService.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Session {
    private final UUID id;
    private final UUID patientId;
    private final UUID psychologistId;
    private final TimeSlot timeSlot;
    private final SessionType type;
    private SessionStatus status;
    private final LocalDateTime createdAt;

    public Session(UUID patientId, UUID psychologistId,
                   TimeSlot timeSlot, SessionType type) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.psychologistId = psychologistId;
        this.timeSlot = timeSlot;
        this.type = type;
        this.status = SessionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void confirm() {
        this.status = SessionStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = SessionStatus.CANCELLED;
    }

    public void complete() {
        this.status = SessionStatus.COMPLETED;
    }

}