package edu.arsw.proyecto.SchedulingService.infrastructure.controller;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.in.BookSessionUseCase;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/sessions")
public class SchedulingController {
    private final BookSessionUseCase bookSessionUseCase;

    public SchedulingController(BookSessionUseCase bookSessionUseCase) {
        this.bookSessionUseCase = bookSessionUseCase;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> book(
            @RequestBody BookSessionDTO command) {
        Session session = bookSessionUseCase.book(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SessionResponse.from(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        bookSessionUseCase.cancel(id);
        return ResponseEntity.noContent().build();
    }
}