package com.touroperator.domain;

import java.util.UUID;

public class BookingExcursion {
    private UUID bookingId;
    private UUID excursionId;

    public BookingExcursion() {}

    public BookingExcursion(UUID bookingId, UUID excursionId) {
        this.bookingId = bookingId;
        this.excursionId = excursionId;
    }

    // Getters and Setters
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public UUID getExcursionId() { return excursionId; }
    public void setExcursionId(UUID excursionId) { this.excursionId = excursionId; }

    @Override
    public String toString() {
        return String.format("BookingExcursion [bookingId=%s, excursionId=%s]", bookingId, excursionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingExcursion that = (BookingExcursion) o;
        return bookingId.equals(that.bookingId) && excursionId.equals(that.excursionId);
    }

    @Override
    public int hashCode() {
        return bookingId.hashCode() + excursionId.hashCode();
    }
}