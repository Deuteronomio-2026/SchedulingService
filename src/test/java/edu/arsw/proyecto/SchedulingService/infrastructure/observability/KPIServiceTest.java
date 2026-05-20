package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KPIService - Unit Tests")
class KPIServiceTest {

    @Mock
    private BookingMetricsService metricsService;

    private KPIService kpiService;

    @BeforeEach
    void setUp() {
        kpiService = new KPIService(metricsService);
    }

    @Test
    @DisplayName("Should return zero rates when no metrics exist")
    void shouldReturnZeroRatesWhenNoMetricsExist() {
        assertEquals(0.0, kpiService.getSuccessRate());
        assertEquals(0.0, kpiService.getRejectionRate());
        assertEquals(0.0, kpiService.getDoubleBookingConflictRate());
        assertEquals(0.0, kpiService.getCancellationRate());
        assertEquals(0.0, kpiService.getRescheduleRate());
        assertEquals(0, kpiService.getAverageBookingLatencyMs());
    }

    @Test
    @DisplayName("Should calculate rates from metrics")
    void shouldCalculateRatesFromMetrics() {
        when(metricsService.getBookingRequestsTotal()).thenReturn(10.0);
        when(metricsService.getBookingSuccessTotal()).thenReturn(6.0);
        when(metricsService.getBookingRejectedTotal()).thenReturn(3.0);
        when(metricsService.getDoubleBookingTotal()).thenReturn(1.0);
        when(metricsService.getSessionCancellationsTotal()).thenReturn(2.0);
        when(metricsService.getSessionReschedulesTotal()).thenReturn(4.0);
        when(metricsService.getActiveOperations()).thenReturn(7);

        assertEquals(60.0, kpiService.getSuccessRate());
        assertEquals(30.0, kpiService.getRejectionRate());
        assertEquals(10.0, kpiService.getDoubleBookingConflictRate());
        assertEquals(25.0, kpiService.getCancellationRate());
        assertEquals(40.0, kpiService.getRescheduleRate());
        assertEquals(1, kpiService.getAverageBookingLatencyMs());
        assertEquals(7, kpiService.getActiveOperations());
    }

    @Test
    @DisplayName("Should expose all summary values")
    void shouldExposeAllSummaryValues() {
        when(metricsService.getBookingRequestsTotal()).thenReturn(8.0);
        when(metricsService.getBookingSuccessTotal()).thenReturn(4.0);
        when(metricsService.getBookingRejectedTotal()).thenReturn(2.0);
        when(metricsService.getDoubleBookingTotal()).thenReturn(1.0);
        when(metricsService.getSessionCancellationsTotal()).thenReturn(1.0);
        when(metricsService.getSessionReschedulesTotal()).thenReturn(3.0);
        when(metricsService.getActiveOperations()).thenReturn(2);

        KPIService.KPISummary summary = kpiService.getSummary();

        assertEquals(50.0, summary.getSuccessRate());
        assertEquals(25.0, summary.getRejectionRate());
        assertEquals(12.5, summary.getDoubleBookingConflictRate());
        assertEquals(20.0, summary.getCancellationRate());
        assertEquals(42.857142857142854, summary.getRescheduleRate());
        assertEquals(1, summary.getAverageLatencyMs());
        assertEquals(2, summary.getActiveOperations());
        assertEquals(8.0, summary.getTotalRequests());
        assertEquals(4.0, summary.getTotalSuccessful());
        assertEquals(1.0, summary.getTotalCancellations());
        assertEquals(3.0, summary.getTotalReschedules());
    }
}
