package edu.arsw.proyecto.SchedulingService.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AsyncConfig - Unit Tests")
class AsyncConfigTest {

    @Test
    @DisplayName("Should create event publisher executor")
    void shouldCreateEventPublisherExecutor() throws InterruptedException {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = config.eventPublisherExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            executor.execute(latch::countDown);

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertTrue(executor.getThreadNamePrefix().startsWith("scheduling-events-"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should expose async executor and exception handler")
    void shouldExposeAsyncExecutorAndExceptionHandler() throws NoSuchMethodException {
        AsyncConfig config = new AsyncConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.getAsyncExecutor();
        try {
            AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
            Method method = AsyncConfigTest.class.getDeclaredMethod("methodForExceptionHandler");

            handler.handleUncaughtException(new RuntimeException("boom"), method);

            assertNotNull(handler);
        } finally {
            executor.shutdown();
        }
    }

    @SuppressWarnings("unused")
    private void methodForExceptionHandler() {
    }
}
