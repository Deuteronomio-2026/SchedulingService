package edu.arsw.proyecto.SchedulingService.domain.service;

import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SchedulingDomainService {

    private final LockManager lockManager;

    public SchedulingDomainService(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public Session createSession(UUID patientId, UUID psychologistId,
                                 TimeSlot slot, SessionType type) {
        boolean acquired = lockManager.acquireLock(psychologistId);
        if (!acquired) {
            throw new SlotNotAvailableException(
                    "No se pudo adquirir el lock para el psicólogo"
            );
        }
        try {
            return new Session(patientId, psychologistId, slot, type);
        } finally {
            lockManager.releaseLock(psychologistId);
        }
    }
}