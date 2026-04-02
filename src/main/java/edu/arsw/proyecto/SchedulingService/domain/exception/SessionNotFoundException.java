package edu.arsw.proyecto.SchedulingService.domain.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message) {
        super(message);
    }
}
