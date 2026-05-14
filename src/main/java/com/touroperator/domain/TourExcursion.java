package com.touroperator.domain;

import java.util.UUID;

public class TourExcursion {
    private UUID tourId;
    private UUID excursionId;

    public TourExcursion() {}

    public TourExcursion(UUID tourId, UUID excursionId) {
        this.tourId = tourId;
        this.excursionId = excursionId;
    }

    // Getters and Setters
    public UUID getTourId() { return tourId; }
    public void setTourId(UUID tourId) { this.tourId = tourId; }

    public UUID getExcursionId() { return excursionId; }
    public void setExcursionId(UUID excursionId) { this.excursionId = excursionId; }

    @Override
    public String toString() {
        return String.format("TourExcursion [tourId=%s, excursionId=%s]", tourId, excursionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TourExcursion that = (TourExcursion) o;
        return tourId.equals(that.tourId) && excursionId.equals(that.excursionId);
    }

    @Override
    public int hashCode() {
        return tourId.hashCode() + excursionId.hashCode();
    }
}