package com.touroperator.dto;

import java.math.BigDecimal;


public class TourReport {

    private String tourName;
    private int totalBookings;
    private int totalTourists;
    private BigDecimal totalRevenue;
    private double fillPercent;

    public TourReport() {}

    public TourReport(String tourName, int totalBookings, int totalTourists,
          BigDecimal totalRevenue, double fillPercent) {
        this.tourName = tourName;
        this.totalBookings = totalBookings;
        this.totalTourists = totalTourists;
        this.totalRevenue = totalRevenue;
        this.fillPercent = fillPercent;
    }

    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public int getTotalTourists() { return totalTourists; }
    public void setTotalTourists(int totalTourists) { this.totalTourists = totalTourists; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getFillPercent() { return fillPercent; }
    public void setFillPercent(double fillPercent) { this.fillPercent = fillPercent; }

    @Override
    public String toString() {
        return String.format("TourReport[tour='%s', bookings=%d, tourists=%d, revenue=%s, fill=%.1f%%]",
              tourName, totalBookings, totalTourists, totalRevenue, fillPercent);
    }
}
