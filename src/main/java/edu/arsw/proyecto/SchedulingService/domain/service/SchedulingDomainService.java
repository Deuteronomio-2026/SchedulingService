package edu.arsw.proyecto.SchedulingService.domain.service;

import edu.arsw.proyecto.SchedulingService.application.port.out.DomainLockPort;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;

import java.util.UUID;

public class SchedulingDomainService {

    private final DomainLockPort lockPort;

    public SchedulingDomainService(DomainLockPort lockPort) {
        this.lockPort = lockPort;
    }

    public Session createSession(UUID patientId, UUID psychologistId,
                                 TimeSlot slot, SessionType type) {
        boolean acquired = lockPort.acquireLock(psychologistId);
        if (!acquired) {
            throw new SlotNotAvailableException(
                    "No se pudo adquirir el lock para el psicólogo"
            );
        }
        try {
            return new Session(patientId, psychologistId, slot, type);
        } finally {
            lockPort.releaseLock(psychologistId);
        }
    }
}