package edu.arsw.proyecto.SchedulingService.infrastructure.controller;

import edu.arsw.proyecto.SchedulingService.application.dto.BookSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.dto.RescheduleSessionDTO;
import edu.arsw.proyecto.SchedulingService.application.port.in.BookSessionUseCase;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.CancelSessionResponse;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.DeleteAllSessionsResponse;
import edu.arsw.proyecto.SchedulingService.infrastructure.controller.dto.SessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sessions")
@Tag(name = "Sessions", description = "Endpoints para gestión de sesiones psicológicas")
public class SchedulingController {
    private final BookSessionUseCase bookSessionUseCase;

    public SchedulingController(BookSessionUseCase bookSessionUseCase) {
        this.bookSessionUseCase = bookSessionUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Crear nueva sesión",
            description = "Reserva una sesión psicológica verificando disponibilidad del psicólogo y previniendo double booking mediante locks distribuidos"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Sesión creada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SessionResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                              "patientId": "550e8400-e29b-41d4-a716-446655440001",
                                              "psychologistId": "550e8400-e29b-41d4-a716-446655440002",
                                              "date": "2026-04-15",
                                              "startTime": "14:00",
                                              "endTime": "15:00",
                                              "type": "VIRTUAL",
                                                                                                                                                                                        "attentionType": "PRIMERA_VEZ",
                                              "status": "CONFIRMED",
                                              "createdAt": "2026-04-03T16:18:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Horario no disponible o datos inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "Horario no disponible"
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<SessionResponse> book(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos de la sesión a reservar",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BookSessionDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "patientId": "550e8400-e29b-41d4-a716-446655440001",
                                              "psychologistId": "550e8400-e29b-41d4-a716-446655440002",
                                              "date": "2026-04-15",
                                                                                                                                                                                        "startTime": "14:00",
                                                                                                                                                                                        "endTime": "15:00",
                                                                                                                                                                                        "type": "VIRTUAL",
                                                                                                                                                                                        "attentionType": "PRIMERA_VEZ"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody BookSessionDTO command) {
        Session session = bookSessionUseCase.book(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SessionResponse.from(session));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancelar sesión",
            description = "Cancela una sesión existente y libera el slot para que pueda ser reservado nuevamente"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión cancelada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CancelSessionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "Sesión cancelada exitosamente"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sesión no encontrada o ya cancelada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "No hay una sesión activa con el ID indicado"
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<CancelSessionResponse> cancel(
            @Parameter(description = "ID de la sesión a cancelar", required = true)
            @PathVariable UUID id) {
        bookSessionUseCase.cancel(id);
        return ResponseEntity.ok(new CancelSessionResponse("Sesión cancelada exitosamente"));
    }

    @DeleteMapping
    @Operation(
            summary = "Eliminar todas las sesiones",
            description = "Borra físicamente todas las sesiones registradas en la base de datos"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Sesiones eliminadas exitosamente",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeleteAllSessionsResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              \"message\": \"Sesiones eliminadas exitosamente\",
                              \"deletedCount\": 12
                            }
                            """)
            )
    )
    public ResponseEntity<DeleteAllSessionsResponse> deleteAll() {
        int deletedCount = bookSessionUseCase.deleteAll();
        return ResponseEntity.ok(new DeleteAllSessionsResponse("Sesiones eliminadas exitosamente", deletedCount));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(
            summary = "Reprogramar sesión",
            description = "Permite cambiar la fecha y hora de una sesión existente, validando disponibilidad del psicólogo"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión reprogramada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SessionResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Nueva franja inválida o no disponible"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sesión no encontrada"
            )
    })
    public ResponseEntity<SessionResponse> reschedule(
            @Parameter(description = "ID de la sesión a reprogramar", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nueva fecha y franja de tiempo en formato HH:mm",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RescheduleSessionDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "date": "2026-04-16",
                                              "startTime": "09:00",
                                              "endTime": "10:00"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody RescheduleSessionDTO command) {
        Session session = bookSessionUseCase.reschedule(id, command);
        return ResponseEntity.ok(SessionResponse.from(session));
    }

    @GetMapping
    @Operation(
            summary = "Listar todas las sesiones",
            description = "Obtiene un listado de todas las sesiones registradas en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de sesiones obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SessionResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            [
                                              {
                                                "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                                "patientId": "550e8400-e29b-41d4-a716-446655440001",
                                                "psychologistId": "550e8400-e29b-41d4-a716-446655440002",
                                                "date": "2026-04-15",
                                                "startTime": "14:00",
                                                "endTime": "15:00",
                                                "type": "VIRTUAL",
                                                                                                                                                                                                "attentionType": "PRIMERA_VEZ",
                                                "status": "CONFIRMED",
                                                "createdAt": "2026-04-03T16:18:00"
                                              }
                                            ]
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<SessionResponse>> getAll() {
        List<SessionResponse> sessions = bookSessionUseCase.findAll()
                .stream()
                .map(SessionResponse::from)
                .toList();
        return ResponseEntity.ok(sessions);
    }
}