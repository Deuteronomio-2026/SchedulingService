package edu.arsw.proyecto.SchedulingService.infrastructure.redis;

import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisSlotLockAdapter - Unit Tests")
class RedisSlotLockAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisSlotLockAdapter adapter;
    private UUID psychologistId;
    private TimeSlot slot;
    private String key;

    @BeforeEach
    void setUp() {
        adapter = new RedisSlotLockAdapter(redisTemplate);
        psychologistId = UUID.randomUUID();
        slot = new TimeSlot(LocalDate.of(2026, 4, 15), LocalTime.of(14, 0), LocalTime.of(15, 0));
        key = "slot:%s:2026-04-15:14:00".formatted(psychologistId);
    }

    @Test
    @DisplayName("Should acquire slot when Redis setIfAbsent succeeds")
    void shouldAcquireSlotWhenRedisSetIfAbsentSucceeds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(key, "locked", 10, TimeUnit.MINUTES)).thenReturn(true);

        assertTrue(adapter.tryLockSlot(psychologistId, slot));
    }

    @Test
    @DisplayName("Should not acquire slot when Redis setIfAbsent fails")
    void shouldNotAcquireSlotWhenRedisSetIfAbsentFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(key, "locked", 10, TimeUnit.MINUTES)).thenReturn(false);

        assertFalse(adapter.tryLockSlot(psychologistId, slot));
    }

    @Test
    @DisplayName("Should not acquire slot when Redis returns null")
    void shouldNotAcquireSlotWhenRedisReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(key, "locked", 10, TimeUnit.MINUTES)).thenReturn(null);

        assertFalse(adapter.tryLockSlot(psychologistId, slot));
    }

    @Test
    @DisplayName("Should write explicit slot lock")
    void shouldWriteExplicitSlotLock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.lockSlot(psychologistId, slot);

        verify(valueOperations).set(key, "locked", 10, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("Should unlock slot")
    void shouldUnlockSlot() {
        adapter.unlockSlot(psychologistId, slot);

        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("Should report locked slot")
    void shouldReportLockedSlot() {
        when(redisTemplate.hasKey(key)).thenReturn(true);

        assertTrue(adapter.isLocked(psychologistId, slot));
    }

    @Test
    @DisplayName("Should report unlocked slot")
    void shouldReportUnlockedSlot() {
        when(redisTemplate.hasKey(key)).thenReturn(null);

        assertFalse(adapter.isLocked(psychologistId, slot));
    }
}
