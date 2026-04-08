package edu.arsw.proyecto.SchedulingService.infrastructure.lock;

import edu.arsw.proyecto.SchedulingService.application.port.out.DomainLockPort;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class InMemoryLockAdapter implements DomainLockPort {

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public boolean acquireLock(UUID psychologistId) {
        ReentrantLock lock = locks.computeIfAbsent(
                psychologistId, id -> new ReentrantLock()
        );
        try {
            return lock.tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void releaseLock(UUID psychologistId) {
        ReentrantLock lock = locks.get(psychologistId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
