package edu.arsw.proyecto.SchedulingService.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeSlot - Unit Tests")
class TimeSlotTest {

    @Test
    @DisplayName("Should create TimeSlot with valid data")
    void shouldCreateTimeSlotWithValidData() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        TimeSlot slot = new TimeSlot(date, startTime, endTime);

        assertNotNull(slot);
        assertEquals(date, slot.getDate());
        assertEquals(startTime, slot.getStartTime());
        assertEquals(endTime, slot.getEndTime());
    }

    @Test
    @DisplayName("Should detect overlap when slots overlap completely")
    void shouldDetectOverlapWhenSlotsOverlapCompletely() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        TimeSlot slot1 = new TimeSlot(date, LocalTime.of(14, 0), LocalTime.of(15, 0));
        TimeSlot slot2 = new TimeSlot(date, LocalTime.of(14, 0), LocalTime.of(15, 0));

        assertTrue(slot1.overlaps(slot2));
        assertTrue(slot2.overlaps(slot1));
    }

    @Test
    @DisplayName("Should detect overlap when slots partially overlap")
    void shouldDetectOverlapWhenSlotsPartiallyOverlap() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        TimeSlot slot1 = new TimeSlot(date, LocalTime.of(14, 0), LocalTime.of(15, 0));
        TimeSlot slot2 = new TimeSlot(date, LocalTime.of(14, 30), LocalTime.of(15, 30));

        assertTrue(slot1.overlaps(slot2));
        assertTrue(slot2.overlaps(slot1));
    }

    @Test
    @DisplayName("Should detect overlap when one slot contains another")
    void shouldDetectOverlapWhenOneSlotContainsAnother() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        TimeSlot slot1 = new TimeSlot(date, LocalTime.of(14, 0), LocalTime.of(16, 0));
        TimeSlot slot2 = new TimeSlot(date, LocalTime.of(14, 30), LocalTime.of(15, 30));

        assertTrue(slot1.overlaps(slot2));
        assertTrue(slot2.overlaps(slot1));
    }

    @Test
    @DisplayName("Should not detect overlap when slots are consecutive")
    void shouldNotDetectOverlapWhenSlotsAreConsecutive() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        TimeSlot slot1 = new TimeSlot(date, LocalTime.of(14, 0), LocalTime.of(15, 0));
        TimeSlot slot2 = new TimeSlot(date, LocalTime.of(15, 0), LocalTime.of(16, 0));

        assertFalse(slot1.overlaps(slot2));
        assertFalse(slot2.overlaps(slot1));
    }

    @Test
    @DisplayName("Should not detect overlap when slots are on different dates")
    void shouldNotDetectOverlapWhenSlotsAreOnDifferentDates() {
        TimeSlot slot1 = new TimeSlot(LocalDate.of(2026, 4, 15), LocalTime.of(14, 0), LocalTime.of(15, 0));
        TimeSlot slot2 = new TimeSlot(LocalDate.of(2026, 4, 16), LocalTime.of(14, 0), LocalTime.of(15, 0));

        assertFalse(slot1.overlaps(slot2));
        assertFalse(slot2.overlaps(slot1));
    }

    @Test
    @DisplayName("Should not detect overlap when slots are completely separate")
    void shouldNotDetectOverlapWhenSlotsAreCompletelySeparate() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        TimeSlot slot1 = new TimeSlot(date, LocalTime.of(10, 0), LocalTime.of(11, 0));
        TimeSlot slot2 = new TimeSlot(date, LocalTime.of(14, 0), LocalTime.of(15, 0));

        assertFalse(slot1.overlaps(slot2));
        assertFalse(slot2.overlaps(slot1));
    }
}
