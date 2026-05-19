package edu.arsw.proyecto.SchedulingService.infrastructure.messaging;

import edu.arsw.proyecto.SchedulingService.application.port.out.EventPublisherPort;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import edu.arsw.proyecto.SchedulingService.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

@Component
public class RabbitMQEventPublisher implements EventPublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private final Executor eventPublisherExecutor;
    private static final String EXCHANGE = RabbitMQConfig.SCHEDULING_EXCHANGE;

    public RabbitMQEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Qualifier("eventPublisherExecutor") Executor eventPublisherExecutor) {
        this.rabbitTemplate = rabbitTemplate;
        this.eventPublisherExecutor = eventPublisherExecutor;
    }

    @Override
    public void publishSessionBooked(Session session) {
        publishAfterCommit(() -> rabbitTemplate.convertAndSend(
                EXCHANGE, "session.booked", session));
    }

    @Override
    public void publishSessionRescheduled(Session session) {
        publishAfterCommit(() -> rabbitTemplate.convertAndSend(
                EXCHANGE, "session.rescheduled", session));
    }

    @Override
    public void publishSessionCancelled(Session session) {
        publishAfterCommit(() -> rabbitTemplate.convertAndSend(
                EXCHANGE, "session.cancelled", session));
    }

    private void publishAfterCommit(Runnable publisher) {
        Runnable asyncPublisher = () -> eventPublisherExecutor.execute(publisher);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    asyncPublisher.run();
                }
            });
            return;
        }

        asyncPublisher.run();
    }
}
