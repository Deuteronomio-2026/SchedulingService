package edu.arsw.proyecto.SchedulingService.application.port.in;

import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// application/port/in/BookSessionCommand.java
public class BookSessionCommand {
    private final UUID patientId;
    private final UUID psychologistId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final SessionType type;
}