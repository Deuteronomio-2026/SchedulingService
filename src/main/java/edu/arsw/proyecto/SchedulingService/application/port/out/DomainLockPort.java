package edu.arsw.proyecto.SchedulingService.application.port.out;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface DomainLockPort {
    <T> Optional<T> withLock(UUID psychologistId, Supplier<T> action);
}
