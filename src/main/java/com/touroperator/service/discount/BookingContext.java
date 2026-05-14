package com.touroperator.service.discount;

import com.touroperator.domain.PromoCode;
import com.touroperator.domain.Tour;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


public class BookingContext {
    private final Tour tour;
    private final int touristCount;
    private final int childCount;
    private final PromoCode promoCode;
    private final LocalDate bookingDate;

    public BookingContext(Tour tour, int touristCount, int childCount,
          PromoCode promoCode) {
        this.tour = tour;
        this.touristCount = touristCount;
        this.childCount = childCount;
        this.promoCode = promoCode;
        this.bookingDate = LocalDate.now();
    }


    public long getDaysUntilTour() {
        return ChronoUnit.DAYS.between(bookingDate, tour.getStartDate());
    }

    public Tour getTour() { return tour; }
    public int getTouristCount() { return touristCount; }
    public int getChildCount() { return childCount; }
    public PromoCode getPromoCode() { return promoCode; }
    public LocalDate getBookingDate() { return bookingDate; }
}
