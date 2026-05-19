package edu.arsw.proyecto.SchedulingService.infrastructure.controller;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.in.BookSessionUseCase;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/diagnostics")
public class ConcurrencyDiagnosticsController {

    private static final int MIN_REQUESTS = 2;
    private static final int MAX_REQUESTS = 200;

    private final BookSessionUseCase bookSessionUseCase;

    public ConcurrencyDiagnosticsController(BookSessionUseCase bookSessionUseCase) {
        this.bookSessionUseCase = bookSessionUseCase;
    }

    @PostMapping("/concurrency-booking")
    public ResponseEntity<ConcurrencyDiagnosticsResponse> runConcurrencyBooking(
            @RequestBody ConcurrencyDiagnosticsRequest request) throws Exception {
        int requestCount = Math.max(MIN_REQUESTS, Math.min(MAX_REQUESTS, request.requestCount()));
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        List<ConcurrencyRequestResult> results = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int index = 1; index <= requestCount; index++) {
            int requestIndex = index;
            futures.add(executor.submit(() -> {
                start.await();
                UUID patientId = UUID.randomUUID();
                long startedAt = System.nanoTime();

                try {
                    Session session = bookSessionUseCase.book(new BookSessionDTO(
                            patientId,
                            request.psychologistId(),
                            request.date(),
                            request.startTime(),
                            request.endTime(),
                            request.type(),
                            request.attentionType()
                    ));
                    long latencyMs = elapsedMs(startedAt);
                    successes.incrementAndGet();
                    addResult(results, new ConcurrencyRequestResult(
                            requestIndex,
                            "success",
                            201,
                            latencyMs,
                            patientId,
                            session.getId(),
                            "Reserva creada"
                    ));
                } catch (SlotNotAvailableException e) {
                    long latencyMs = elapsedMs(startedAt);
                    rejections.incrementAndGet();
                    addResult(results, new ConcurrencyRequestResult(
                            requestIndex,
                            "rejected",
                            400,
                            latencyMs,
                            patientId,
                            null,
                            e.getMessage()
                    ));
                } catch (Exception e) {
                    long latencyMs = elapsedMs(startedAt);
                    errors.incrementAndGet();
                    addResult(results, new ConcurrencyRequestResult(
                            requestIndex,
                            "error",
                            500,
                            latencyMs,
                            patientId,
                            null,
                            e.getClass().getSimpleName() + ": " + e.getMessage()
                    ));
                }
                return null;
            }));
        }

        long startedAt = System.nanoTime();
        start.countDown();
        try {
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        long durationMs = elapsedMs(startedAt);
        List<ConcurrencyRequestResult> sortedResults;
        synchronized (results) {
            sortedResults = results.stream()
                    .sorted(Comparator.comparingInt(ConcurrencyRequestResult::index))
                    .toList();
        }

        return ResponseEntity.ok(new ConcurrencyDiagnosticsResponse(
                requestCount,
                successes.get(),
                rejections.get(),
                errors.get(),
                durationMs,
                durationMs < 500 && successes.get() == 1 && rejections.get() == requestCount - 1 && errors.get() == 0,
                sortedResults
        ));
    }

    private static void addResult(List<ConcurrencyRequestResult> results, ConcurrencyRequestResult result) {
        synchronized (results) {
            results.add(result);
        }
    }

    private static long elapsedMs(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    public record ConcurrencyDiagnosticsRequest(
            int requestCount,
            UUID psychologistId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            SessionType type,
            SessionAttentionType attentionType
    ) {
    }

    public record ConcurrencyDiagnosticsResponse(
            int totalRequests,
            int successes,
            int rejections,
            int errors,
            long durationMs,
            boolean scenarioMet,
            List<ConcurrencyRequestResult> results
    ) {
    }

    public record ConcurrencyRequestResult(
            int index,
            String kind,
            int httpStatus,
            long latencyMs,
            UUID patientId,
            UUID sessionId,
            String message
    ) {
    }
}
