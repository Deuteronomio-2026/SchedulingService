package edu.arsw.proyecto.SchedulingService.infrastructure.messaging;

import edu.arsw.proyecto.SchedulingService.application.port.out.EventPublisherPort;
import edu.arsw.proyecto.SchedulingService.domain.model.Session;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQEventPublisher implements EventPublisherPort {

    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "scheduling.exchange";

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishSessionBooked(Session session) {
        rabbitTemplate.convertAndSend(
                EXCHANGE, "session.booked", session
        );
    }

    @Override
    public void publishSessionCancelled(Session session) {
        rabbitTemplate.convertAndSend(
                EXCHANGE, "session.cancelled", session
        );
    }
}