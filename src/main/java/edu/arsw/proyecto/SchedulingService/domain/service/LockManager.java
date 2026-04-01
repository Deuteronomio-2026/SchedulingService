package edu.arsw.proyecto.SchedulingService.domain.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {

    private static LockManager instance;
    private final ConcurrentHashMap<UUID, ReentrantLock> locks
            = new ConcurrentHashMap<>();

    public static synchronized LockManager getInstance() {
        if (instance == null) instance = new LockManager();
        return instance;
    }

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

    public void releaseLock(UUID psychologistId) {
        ReentrantLock lock = locks.get(psychologistId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}