package edu.arsw.proyecto.SchedulingService.domain.exception;

public class SlotNotAvailableException extends RuntimeException
{
    public SlotNotAvailableException(String message) {
        super(message);
    }
}
