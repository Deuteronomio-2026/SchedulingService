package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BookingEndpointMetricsFilter extends OncePerRequestFilter {

    private static final String BOOKING_PATH = "/sessions";
    private static final String BOOKING_METHOD = "POST";

    private final BookingMetricsService metricsService;

    public BookingEndpointMetricsFilter(BookingMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !BOOKING_METHOD.equalsIgnoreCase(request.getMethod())
                || !BOOKING_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Timer.Sample sample = metricsService.startBookingEndpointRequest();
        boolean failedBeforeResponse = false;

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failedBeforeResponse = true;
            throw e;
        } finally {
            int status = failedBeforeResponse
                    ? HttpStatus.INTERNAL_SERVER_ERROR.value()
                    : response.getStatus();
            metricsService.stopBookingEndpointRequest(sample, status);
        }
    }
}
