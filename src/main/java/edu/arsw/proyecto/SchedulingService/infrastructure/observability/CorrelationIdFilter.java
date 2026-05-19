package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements WebFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlationId";
    private static final String REQUEST_ID_MDC = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;
        String requestId = UUID.randomUUID().toString();
        final String finalRequestId = requestId;

        return chain.filter(exchange)
                .doOnSubscribe(s -> {
                    MDC.put(CORRELATION_ID_MDC, finalCorrelationId);
                    MDC.put(REQUEST_ID_MDC, finalRequestId);
                    exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
                })
                .doFinally(signalType -> MDC.clear());
    }
}
