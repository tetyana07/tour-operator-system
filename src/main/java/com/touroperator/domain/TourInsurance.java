package com.touroperator.domain;

import java.util.UUID;

public class TourInsurance {
    private UUID tourId;
    private UUID insuranceId;

    public TourInsurance() {}

    public TourInsurance(UUID tourId, UUID insuranceId) {
        this.tourId = tourId;
        this.insuranceId = insuranceId;
    }

    // Getters and Setters
    public UUID getTourId() { return tourId; }
    public void setTourId(UUID tourId) { this.tourId = tourId; }

    public UUID getInsuranceId() { return insuranceId; }
    public void setInsuranceId(UUID insuranceId) { this.insuranceId = insuranceId; }

    @Override
    public String toString() {
        return String.format("TourInsurance [tourId=%s, insuranceId=%s]", tourId, insuranceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TourInsurance that = (TourInsurance) o;
        return tourId.equals(that.tourId) && insuranceId.equals(that.insuranceId);
    }

    @Override
    public int hashCode() {
        return tourId.hashCode() + insuranceId.hashCode();
    }
}