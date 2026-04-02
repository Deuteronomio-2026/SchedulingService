package edu.arsw.proyecto.SchedulingService.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Session {
    private UUID id;
    private UUID patientId;
    private UUID psychologistId;
    private TimeSlot timeSlot;
    private SessionType type;
    private SessionStatus status;
    private LocalDateTime createdAt;

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