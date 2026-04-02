package edu.arsw.proyecto.SchedulingService.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface JpaSessionSpringRepository extends JpaRepository<SessionJpaEntity, UUID> {
    boolean existsByPsychologistIdAndDateAndStartTime(
            UUID psychologistId,
            LocalDate date,
            LocalTime startTime
    );
}
