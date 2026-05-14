package com.touroperator.domain;

import java.util.UUID;

public class TourTransfer {
    private UUID tourId;
    private UUID transferId;

    public TourTransfer() {}

    public TourTransfer(UUID tourId, UUID transferId) {
        this.tourId = tourId;
        this.transferId = transferId;
    }

    // Getters and Setters
    public UUID getTourId() { return tourId; }
    public void setTourId(UUID tourId) { this.tourId = tourId; }

    public UUID getTransferId() { return transferId; }
    public void setTransferId(UUID transferId) { this.transferId = transferId; }

    @Override
    public String toString() {
        return String.format("TourTransfer [tourId=%s, transferId=%s]", tourId, transferId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TourTransfer that = (TourTransfer) o;
        return tourId.equals(that.tourId) && transferId.equals(that.transferId);
    }

    @Override
    public int hashCode() {
        return tourId.hashCode() + transferId.hashCode();
    }
}