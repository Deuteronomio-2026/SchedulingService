package edu.arsw.proyecto.SchedulingService.application.dto;

import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookSessionDTO(
        UUID patientId,
        UUID psychologistId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        SessionType type
) {
}
