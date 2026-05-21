package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import io.micrometer.core.instrument.Timer;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingEndpointMetricsFilter - Unit Tests")
class BookingEndpointMetricsFilterTest {

    @Mock
    private BookingMetricsService metricsService;

    @Mock
    private Timer.Sample sample;

    @Test
    @DisplayName("Should record booking endpoint status")
    void shouldRecordBookingEndpointStatus() throws ServletException, IOException {
        BookingEndpointMetricsFilter filter = new BookingEndpointMetricsFilter(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sessions");
        request.setServletPath("/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(metricsService.startBookingEndpointRequest()).thenReturn(sample);

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(201));

        verify(metricsService).startBookingEndpointRequest();
        verify(metricsService).stopBookingEndpointRequest(sample, 201);
    }

    @Test
    @DisplayName("Should ignore non booking requests")
    void shouldIgnoreNonBookingRequests() throws ServletException, IOException {
        BookingEndpointMetricsFilter filter = new BookingEndpointMetricsFilter(metricsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sessions");
        request.setServletPath("/sessions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(200));

        verify(metricsService, never()).startBookingEndpointRequest();
    }
}
