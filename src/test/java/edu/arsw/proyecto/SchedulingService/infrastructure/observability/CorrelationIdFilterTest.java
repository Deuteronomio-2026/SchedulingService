package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("CorrelationIdFilter - Unit Tests")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should reuse incoming correlation id")
    void shouldReuseIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/sessions")
                        .header("X-Correlation-ID", "existing-id")
                        .build());
        WebFilterChain chain = serverWebExchange -> reactor.core.publisher.Mono.empty();

        filter.filter(exchange, chain).block();

        assertEquals("existing-id", exchange.getResponse().getHeaders().getFirst("X-Correlation-ID"));
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("Should generate missing correlation id")
    void shouldGenerateMissingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/sessions")
                        .header("X-Correlation-ID", " ")
                        .build());
        WebFilterChain chain = serverWebExchange -> reactor.core.publisher.Mono.empty();

        filter.filter(exchange, chain).block();

        String generated = exchange.getResponse().getHeaders().getFirst("X-Correlation-ID");
        assertFalse(generated == null || generated.isBlank());
        assertFalse(" ".equals(generated));
        assertEquals(1, exchange.getResponse().getHeaders().get("X-Correlation-ID").size());
    }
}
