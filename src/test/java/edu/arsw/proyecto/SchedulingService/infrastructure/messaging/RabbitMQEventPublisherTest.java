package edu.arsw.proyecto.SchedulingService.infrastructure.messaging;

import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionAttentionType;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionStatus;
import edu.arsw.proyecto.SchedulingService.domain.model.SessionType;
import edu.arsw.proyecto.SchedulingService.domain.model.TimeSlot;
import edu.arsw.proyecto.SchedulingService.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQEventPublisher - Unit Tests")
class RabbitMQEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQEventPublisher publisher;
    private Session session;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMQEventPublisher(rabbitTemplate, Runnable::run);
        session = sampleSession();
    }

    @Test
    @DisplayName("Should publish session events immediately when no transaction exists")
    void shouldPublishSessionEventsImmediatelyWhenNoTransactionExists() {
        publisher.publishSessionBooked(session);
        publisher.publishSessionRescheduled(session);
        publisher.publishSessionCancelled(session);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SCHEDULING_EXCHANGE, "session.booked", session);
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SCHEDULING_EXCHANGE, "session.rescheduled", session);
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.SCHEDULING_EXCHANGE, "session.cancelled", session);
    }

    @Test
    @DisplayName("Should defer publishing until transaction commit")
    void shouldDeferPublishingUntilTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishSessionBooked(session);

            verifyNoInteractions(rabbitTemplate);

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(rabbitTemplate).convertAndSend(
                    RabbitMQConfig.SCHEDULING_EXCHANGE, "session.booked", session);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static Session sampleSession() {
        return Session.reconstituteFromPersistence(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new TimeSlot(LocalDate.of(2026, 4, 15), LocalTime.of(14, 0), LocalTime.of(15, 0)),
                SessionType.VIRTUAL,
                SessionAttentionType.PRIMERA_VEZ,
                SessionStatus.CONFIRMED,
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );
    }
}
