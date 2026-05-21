package edu.arsw.proyecto.SchedulingService.infrastructure.controller;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.dto.RescheduleSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.in.BookSessionUseCase;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ConcurrencyDiagnosticsController - Unit Tests")
class ConcurrencyDiagnosticsControllerTest {

    @Test
    @DisplayName("Should collect success rejection and error results")
    void shouldCollectSuccessRejectionAndErrorResults() throws Exception {
        ConcurrencyDiagnosticsController controller = new ConcurrencyDiagnosticsController(
                useCaseWithOutcomes(Outcome.SUCCESS, Outcome.REJECTION, Outcome.ERROR));

        ResponseEntity<ConcurrencyDiagnosticsController.ConcurrencyDiagnosticsResponse> response =
                controller.runConcurrencyBooking(request(3));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConcurrencyDiagnosticsController.ConcurrencyDiagnosticsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(3, body.totalRequests());
        assertEquals(1, body.successes());
        assertEquals(1, body.rejections());
        assertEquals(1, body.errors());
        assertFalse(body.scenarioMet());
        assertEquals(3, body.results().size());
        assertEquals(List.of(1, 2, 3), body.results().stream()
                .map(ConcurrencyDiagnosticsController.ConcurrencyRequestResult::index)
                .toList());

        Map<String, Long> kinds = body.results().stream()
                .collect(Collectors.groupingBy(
                        ConcurrencyDiagnosticsController.ConcurrencyRequestResult::kind,
                        Collectors.counting()));
        assertEquals(1L, kinds.get("success"));
        assertEquals(1L, kinds.get("rejected"));
        assertEquals(1L, kinds.get("error"));
        assertEquals(409, body.results().stream()
                .filter(result -> "rejected".equals(result.kind()))
                .findFirst()
                .orElseThrow()
                .httpStatus());
    }

    @Test
    @DisplayName("Should clamp request count to minimum")
    void shouldClampRequestCountToMinimum() throws Exception {
        ConcurrencyDiagnosticsController controller = new ConcurrencyDiagnosticsController(
                useCaseWithOutcomes(Outcome.SUCCESS, Outcome.REJECTION));

        ResponseEntity<ConcurrencyDiagnosticsController.ConcurrencyDiagnosticsResponse> response =
                controller.runConcurrencyBooking(request(1));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ConcurrencyDiagnosticsController.ConcurrencyDiagnosticsResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.totalRequests());
        assertEquals(1, body.successes());
        assertEquals(1, body.rejections());
        assertEquals(0, body.errors());
    }

    @Test
    @DisplayName("Should stop executor when waiting thread is interrupted")
    void shouldStopExecutorWhenWaitingThreadIsInterrupted() {
        ConcurrencyDiagnosticsController controller = new ConcurrencyDiagnosticsController(
                useCaseWithOutcomes(Outcome.SUCCESS, Outcome.REJECTION));

        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> controller.runConcurrencyBooking(request(2)));
        Thread.interrupted();
    }

    private static ConcurrencyDiagnosticsController.ConcurrencyDiagnosticsRequest request(int requestCount) {
        return new ConcurrencyDiagnosticsController.ConcurrencyDiagnosticsRequest(
                requestCount,
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 15),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ
        );
    }

    private static BookSessionUseCase useCaseWithOutcomes(Outcome... outcomes) {
        AtomicInteger calls = new AtomicInteger();
        return new BookSessionUseCase() {
            @Override
            public Session book(BookSessionDTO command) {
                Outcome outcome = outcomes[calls.getAndIncrement()];
                if (outcome == Outcome.REJECTION) {
                    throw new SlotNotAvailableException("Horario ya reservado");
                }
                if (outcome == Outcome.ERROR) {
                    throw new IllegalStateException("boom");
                }
                return new Session(
                        command.patientId(),
                        command.psychologistId(),
                        new TimeSlot(command.date(), command.startTime(), command.endTime()),
                        command.type(),
                        command.attentionType()
                );
            }

            @Override
            public Session reschedule(UUID sessionId, RescheduleSessionDTO command) {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public void cancel(UUID sessionId) {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public int deleteAll() {
                throw new UnsupportedOperationException("Not used by this test");
            }

            @Override
            public List<Session> findAll() {
                throw new UnsupportedOperationException("Not used by this test");
            }
        };
    }

    private enum Outcome {
        SUCCESS,
        REJECTION,
        ERROR
    }
}
