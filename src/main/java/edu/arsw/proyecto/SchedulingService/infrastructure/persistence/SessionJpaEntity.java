package edu.arsw.proyecto.SchedulingService.infrastructure.persistence;

import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

// infrastructure/persistence/SessionJpaEntity.java
@Entity
@Getter
@Setter
@Table(
        name = "sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sessions_psychologist_slot",
                columnNames = {"psychologist_id", "date", "start_time"}
        )
)
public class SessionJpaEntity {

    @Id
    private UUID id;
    private UUID patientId;
    private UUID psychologistId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private SessionType type;

    @Enumerated(EnumType.STRING)
    private SessionAttentionType attentionType;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private LocalDateTime createdAt;


}
