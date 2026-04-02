package edu.arsw.proyecto.SchedulingService.application.port.out;

import java.util.UUID;

public interface DomainLockPort {
    boolean acquireLock(UUID psychologistId);
    void releaseLock(UUID psychologistId);
}
