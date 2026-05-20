package edu.arsw.proyecto.SchedulingService.infrastructure.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("InMemoryLockAdapter - Unit Tests")
class InMemoryLockAdapterTest {

    private final InMemoryLockAdapter adapter = new InMemoryLockAdapter();

    @Test
    @DisplayName("Should execute action and release lock")
    void shouldExecuteActionAndReleaseLock() {
        UUID psychologistId = UUID.randomUUID();
        AtomicInteger executions = new AtomicInteger();

        Optional<String> first = adapter.withLock(psychologistId, () -> {
            executions.incrementAndGet();
            return "created";
        });
        Optional<String> second = adapter.withLock(psychologistId, () -> {
            executions.incrementAndGet();
            return "created-again";
        });

        assertEquals(Optional.of("created"), first);
        assertEquals(Optional.of("created-again"), second);
        assertEquals(2, executions.get());
    }

    @Test
    @DisplayName("Should return empty when action returns null")
    void shouldReturnEmptyWhenActionReturnsNull() {
        Optional<Object> result = adapter.withLock(UUID.randomUUID(), () -> null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should preserve interrupted status when interrupted while acquiring")
    void shouldPreserveInterruptedStatusWhenInterruptedWhileAcquiring() {
        Thread.currentThread().interrupt();

        Optional<Object> result = adapter.withLock(UUID.randomUUID(), () -> {
            fail("Action should not be executed when lock acquisition is interrupted");
            return null;
        });

        assertTrue(result.isEmpty());
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }
}
