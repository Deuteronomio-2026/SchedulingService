package edu.arsw.proyecto.SchedulingService.infrastructure.observability;

import org.springframework.stereotype.Service;

@Service
public class KPIService {

    private final BookingMetricsService metricsService;

    public KPIService(BookingMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * KPI 1: Success Rate
     * Formula: (booking_success_total / booking_requests_total) * 100
     * Returns percentage of successful bookings
     */
    public double getSuccessRate() {
        double totalRequests = metricsService.getBookingRequestsTotal();
        if (totalRequests == 0) {
            return 0.0;
        }
        double successCount = metricsService.getBookingSuccessTotal();
        return (successCount / totalRequests) * 100;
    }

    /**
     * KPI 2: Rejection Rate
     * Formula: (booking_rejected_total / booking_requests_total) * 100
     * Returns percentage of rejected bookings
     */
    public double getRejectionRate() {
        double totalRequests = metricsService.getBookingRequestsTotal();
        if (totalRequests == 0) {
            return 0.0;
        }
        double rejectedCount = metricsService.getBookingRejectedTotal();
        return (rejectedCount / totalRequests) * 100;
    }

    /**
     * KPI 3: Double Booking Conflict Rate
     * Formula: (double_booking_total / booking_requests_total) * 100
     * Returns percentage of double booking conflicts
     */
    public double getDoubleBookingConflictRate() {
        double totalRequests = metricsService.getBookingRequestsTotal();
        if (totalRequests == 0) {
            return 0.0;
        }
        double doubleBookings = metricsService.getDoubleBookingTotal();
        return (doubleBookings / totalRequests) * 100;
    }

    /**
     * KPI 4: Cancellation Rate
     * Formula: (session_cancellations_total / (session_cancellations_total + successful_sessions)) * 100
     * Returns percentage of sessions cancelled vs completed
     */
    public double getCancellationRate() {
        double totalCancellations = metricsService.getSessionCancellationsTotal();
        double totalSuccessful = metricsService.getBookingSuccessTotal();
        double totalSessions = totalCancellations + totalSuccessful;

        if (totalSessions == 0) {
            return 0.0;
        }
        return (totalCancellations / totalSessions) * 100;
    }

    /**
     * KPI 5: Reschedule Rate
     * Formula: (session_reschedules_total / (session_reschedules_total + successful_sessions)) * 100
     * Returns percentage of sessions rescheduled
     */
    public double getRescheduleRate() {
        double totalReschedules = metricsService.getSessionReschedulesTotal();
        double totalSuccessful = metricsService.getBookingSuccessTotal();
        double totalSessions = totalReschedules + totalSuccessful;

        if (totalSessions == 0) {
            return 0.0;
        }
        return (totalReschedules / totalSessions) * 100;
    }

    /**
     * Performance KPI: Average booking latency
     * Returns average time to complete a booking in milliseconds
     */
    public long getAverageBookingLatencyMs() {
        double totalRequests = metricsService.getBookingRequestsTotal();
        if (totalRequests == 0) {
            return 0;
        }
        return Math.round(metricsService.getBookingSuccessTotal() / totalRequests);
    }

    /**
     * Operational KPI: Peak concurrent operations
     * Returns current number of active operations
     */
    public int getActiveOperations() {
        return metricsService.getActiveOperations();
    }

    /**
     * Summary of all KPIs
     */
    public KPISummary getSummary() {
        return new KPISummary(
                getSuccessRate(),
                getRejectionRate(),
                getDoubleBookingConflictRate(),
                getCancellationRate(),
                getRescheduleRate(),
                getAverageBookingLatencyMs(),
                getActiveOperations(),
                metricsService.getBookingRequestsTotal(),
                metricsService.getBookingSuccessTotal(),
                metricsService.getSessionCancellationsTotal(),
                metricsService.getSessionReschedulesTotal()
        );
    }

    /**
     * Data class for KPI summary
     */
    public static class KPISummary {
        private final double successRate;
        private final double rejectionRate;
        private final double doubleBookingConflictRate;
        private final double cancellationRate;
        private final double rescheduleRate;
        private final long averageLatencyMs;
        private final int activeOperations;
        private final double totalRequests;
        private final double totalSuccessful;
        private final double totalCancellations;
        private final double totalReschedules;

        public KPISummary(double successRate, double rejectionRate, double doubleBookingConflictRate,
                          double cancellationRate, double rescheduleRate, long averageLatencyMs,
                          int activeOperations, double totalRequests, double totalSuccessful,
                          double totalCancellations, double totalReschedules) {
            this.successRate = successRate;
            this.rejectionRate = rejectionRate;
            this.doubleBookingConflictRate = doubleBookingConflictRate;
            this.cancellationRate = cancellationRate;
            this.rescheduleRate = rescheduleRate;
            this.averageLatencyMs = averageLatencyMs;
            this.activeOperations = activeOperations;
            this.totalRequests = totalRequests;
            this.totalSuccessful = totalSuccessful;
            this.totalCancellations = totalCancellations;
            this.totalReschedules = totalReschedules;
        }

        // Getters
        public double getSuccessRate() { return successRate; }
        public double getRejectionRate() { return rejectionRate; }
        public double getDoubleBookingConflictRate() { return doubleBookingConflictRate; }
        public double getCancellationRate() { return cancellationRate; }
        public double getRescheduleRate() { return rescheduleRate; }
        public long getAverageLatencyMs() { return averageLatencyMs; }
        public int getActiveOperations() { return activeOperations; }
        public double getTotalRequests() { return totalRequests; }
        public double getTotalSuccessful() { return totalSuccessful; }
        public double getTotalCancellations() { return totalCancellations; }
        public double getTotalReschedules() { return totalReschedules; }
    }
}
