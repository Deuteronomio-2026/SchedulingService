package edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
public class SessionResponse {
    private final UUID id;
    private final UUID patientId;
    private final UUID psychologistId;
    private final LocalDate date;
    @JsonFormat(pattern = "HH:mm")
    private final LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private final LocalTime endTime;
    private final SessionType type;
    private final SessionAttentionType attentionType;
    private final SessionStatus status;
    private final LocalDateTime createdAt;

    private SessionResponse(UUID id, UUID patientId, UUID psychologistId,
                           LocalDate date, LocalTime startTime, LocalTime endTime,
                           SessionType type, SessionAttentionType attentionType,
                           SessionStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.psychologistId = psychologistId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.attentionType = attentionType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getPatientId(),
                session.getPsychologistId(),
                session.getTimeSlot().getDate(),
                session.getTimeSlot().getStartTime(),
                session.getTimeSlot().getEndTime(),
                session.getType(),
                session.getAttentionType(),
                session.getStatus(),
                session.getCreatedAt()
        );
    }
}
