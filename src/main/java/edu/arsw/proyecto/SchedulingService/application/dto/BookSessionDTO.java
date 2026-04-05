package edu.arsw.proyecto.SchedulingService.application.dto;

import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookSessionDTO(
        UUID patientId,
        UUID psychologistId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        SessionType type,
        SessionAttentionType attentionType
) {
}
