package edu.arsw.proyecto.SchedulingService.application.port.out;

import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import java.util.UUID;

public interface SlotLockPort {
    boolean tryLockSlot(UUID psychologistId, TimeSlot slot);
    void lockSlot(UUID psychologistId, TimeSlot slot);
    void unlockSlot(UUID psychologistId, TimeSlot slot);
    boolean isLocked(UUID psychologistId, TimeSlot slot);
}
