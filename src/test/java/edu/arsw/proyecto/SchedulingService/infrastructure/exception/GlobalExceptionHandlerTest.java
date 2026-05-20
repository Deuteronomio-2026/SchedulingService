package edu.arsw.proyecto.SchedulingService.infrastructure.exception;

import edu.arsw.proyecto.SchedulingService.domain.exception.SessionNotFoundException;
import edu.arsw.proyecto.SchedulingService.domain.exception.SlotNotAvailableException;
import edu.arsw.proyecto.SchedulingService.infrastructure.observability.LoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private LoggingService loggingService;

    @Mock
    private WebRequest webRequest;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(loggingService);
        when(webRequest.getDescription(false)).thenReturn("uri=/sessions");
    }

    @Test
    @DisplayName("Should handle slot not available")
    void shouldHandleSlotNotAvailable() {
        ResponseEntity<Object> response = handler.handleSlotNotAvailableException(
                new SlotNotAvailableException("Horario ya reservado"), webRequest);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Horario ya reservado");
        verifyLogged("SlotNotAvailableException", "Horario ya reservado");
    }

    @Test
    @DisplayName("Should handle illegal argument")
    void shouldHandleIllegalArgument() {
        ResponseEntity<Object> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Datos invalidos"), webRequest);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Datos invalidos");
        verifyLogged("IllegalArgumentException", "Datos invalidos");
    }

    @Test
    @DisplayName("Should handle illegal state")
    void shouldHandleIllegalState() {
        ResponseEntity<Object> response = handler.handleIllegalStateException(
                new IllegalStateException("Estado invalido"), webRequest);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Estado invalido");
        verifyLogged("IllegalStateException", "Estado invalido");
    }

    @Test
    @DisplayName("Should handle session not found")
    void shouldHandleSessionNotFound() {
        ResponseEntity<Object> response = handler.handleSessionNotFoundException(
                new SessionNotFoundException("Sesion no encontrada"), webRequest);

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Sesion no encontrada");
        verifyLogged("SessionNotFoundException", "Sesion no encontrada");
    }

    @Test
    @DisplayName("Should handle malformed JSON with default message")
    void shouldHandleMalformedJsonWithDefaultMessage() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Cannot read", new IllegalArgumentException("other field"),
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadableException(exception, webRequest);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Cuerpo JSON inválido");
        verifyLogged("HttpMessageNotReadableException", "Cuerpo JSON inválido");
    }

    @Test
    @DisplayName("Should handle malformed time JSON")
    void shouldHandleMalformedTimeJson() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Cannot read", new IllegalArgumentException("startTime value is invalid"),
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadableException(exception, webRequest);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Formato de hora inválido. Usa HH:mm, por ejemplo 09:00");
        verifyLogged("HttpMessageNotReadableException", "Formato de hora inválido. Usa HH:mm, por ejemplo 09:00");
    }

    @Test
    @DisplayName("Should handle unexpected exception")
    void shouldHandleUnexpectedException() {
        ResponseEntity<Object> response = handler.handleGlobalException(
                new RuntimeException("boom"), webRequest);

        assertErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error inesperado");
        verifyLogged("RuntimeException", "boom");
    }

    @SuppressWarnings("unchecked")
    private static void assertErrorResponse(ResponseEntity<Object> response, HttpStatus status, String message) {
        assertEquals(status, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(status.value(), body.get("status"));
        assertEquals(status.getReasonPhrase(), body.get("error"));
        assertEquals(message, body.get("message"));
        assertEquals("/sessions", body.get("path"));
    }

    private void verifyLogged(String errorType, String message) {
        verify(loggingService).logBookingError(
                any(UUID.class), any(UUID.class), eq(errorType), eq(message), any(String.class));
    }
}
