package edu.arsw.proyecto.SchedulingService.infrastructure.lock;

import edu.arsw.proyecto.SchedulingService.application.port.out.DomainLockPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class InMemoryLockAdapter implements DomainLockPort {

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> Optional<T> withLock(UUID psychologistId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(
                psychologistId, id -> new ReentrantLock()
        );
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, TimeUnit.SECONDS);
            if (!acquired) {
                return Optional.empty();
            }
            return Optional.ofNullable(action.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }
}
