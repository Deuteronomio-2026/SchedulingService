package edu.arsw.proyecto.SchedulingService.infrastructure.redis;

import edu.arsw.proyecto.SchedulingService.application.port.out.SlotLockPort;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisSlotLockAdapter implements SlotLockPort {

    private final StringRedisTemplate redisTemplate;
    private static final long TTL_MINUTES = 10;

    public RedisSlotLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(UUID psychologistId, TimeSlot slot) {
        return String.format("slot:%s:%s:%s",
                psychologistId, slot.getDate(), slot.getStartTime());
    }

    @Override
    public void lockSlot(UUID psychologistId, TimeSlot slot) {
        redisTemplate.opsForValue().set(
                buildKey(psychologistId, slot),
                "locked",
                TTL_MINUTES, TimeUnit.MINUTES
        );
    }

    @Override
    public void unlockSlot(UUID psychologistId, TimeSlot slot) {
        redisTemplate.delete(buildKey(psychologistId, slot));
    }

    @Override
    public boolean isLocked(UUID psychologistId, TimeSlot slot) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(buildKey(psychologistId, slot))
        );
    }
}