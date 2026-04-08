package edu.arsw.proyecto.SchedulingService.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SCHEDULING_EXCHANGE = "scheduling.exchange";
    public static final String SESSION_BOOKED_QUEUE = "scheduling.session.booked.queue";
    public static final String SESSION_CANCELLED_QUEUE = "scheduling.session.cancelled.queue";
    public static final String SESSION_RESCHEDULED_QUEUE = "scheduling.session.rescheduled.queue";

    @Bean
    public TopicExchange schedulingExchange() {
        return new TopicExchange(SCHEDULING_EXCHANGE, true, false);
    }

    @Bean
    public Queue sessionBookedQueue() {
        return new Queue(SESSION_BOOKED_QUEUE, true);
    }

    @Bean
    public Queue sessionCancelledQueue() {
        return new Queue(SESSION_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue sessionRescheduledQueue() {
        return new Queue(SESSION_RESCHEDULED_QUEUE, true);
    }

    @Bean
    public Binding sessionBookedBinding(Queue sessionBookedQueue, TopicExchange schedulingExchange) {
        return BindingBuilder.bind(sessionBookedQueue)
                .to(schedulingExchange)
                .with("session.booked");
    }

    @Bean
    public Binding sessionCancelledBinding(Queue sessionCancelledQueue, TopicExchange schedulingExchange) {
        return BindingBuilder.bind(sessionCancelledQueue)
                .to(schedulingExchange)
                .with("session.cancelled");
    }

    @Bean
    public Binding sessionRescheduledBinding(Queue sessionRescheduledQueue, TopicExchange schedulingExchange) {
        return BindingBuilder.bind(sessionRescheduledQueue)
                .to(schedulingExchange)
                .with("session.rescheduled");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
