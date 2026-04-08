package edu.arsw.proyecto.SchedulingService.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Session {
    private final UUID id;
    private final UUID patientId;
    private final UUID psychologistId;
    private TimeSlot timeSlot;
    private final SessionType type;
    private final SessionAttentionType attentionType;
    private SessionStatus status;
    private final LocalDateTime createdAt;

    public Session(UUID patientId, UUID psychologistId,
                   TimeSlot timeSlot, SessionType type, SessionAttentionType attentionType) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.psychologistId = psychologistId;
        this.timeSlot = timeSlot;
        this.type = type;
        this.attentionType = attentionType;
        this.status = SessionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    private Session(UUID id, UUID patientId, UUID psychologistId,
                    TimeSlot timeSlot, SessionType type, SessionAttentionType attentionType, SessionStatus status,
                    LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.psychologistId = psychologistId;
        this.timeSlot = timeSlot;
        this.type = type;
        this.attentionType = attentionType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Session reconstituteFromPersistence(UUID id, UUID patientId, UUID psychologistId,
                                                       TimeSlot timeSlot, SessionType type, SessionAttentionType attentionType,
                                                       SessionStatus status, LocalDateTime createdAt) {
        return new Session(id, patientId, psychologistId, timeSlot, type, attentionType, status, createdAt);
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

    public void reschedule(TimeSlot newTimeSlot) {
        if (this.status == SessionStatus.CANCELLED || this.status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("No se puede reprogramar una sesión cancelada o completada");
        }
        this.timeSlot = newTimeSlot;
    }

}