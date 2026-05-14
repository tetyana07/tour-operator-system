package com.touroperator.domain;

import java.util.UUID;

public class BookingTransfer {
    private UUID bookingId;
    private UUID transferId;

    public BookingTransfer() {}

    public BookingTransfer(UUID bookingId, UUID transferId) {
        this.bookingId = bookingId;
        this.transferId = transferId;
    }

    // Getters and Setters
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public UUID getTransferId() { return transferId; }
    public void setTransferId(UUID transferId) { this.transferId = transferId; }

    @Override
    public String toString() {
        return String.format("BookingTransfer [bookingId=%s, transferId=%s]", bookingId, transferId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingTransfer that = (BookingTransfer) o;
        return bookingId.equals(that.bookingId) && transferId.equals(that.transferId);
    }

    @Override
    public int hashCode() {
        return bookingId.hashCode() + transferId.hashCode();
    }
}