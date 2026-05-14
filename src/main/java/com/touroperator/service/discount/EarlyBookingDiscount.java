package com.touroperator.service.discount;

import java.math.BigDecimal;


public class EarlyBookingDiscount implements DiscountStrategy {

    private static final int MIN_DAYS_AHEAD = 30;
    private static final double DISCOUNT_RATE = 0.10;

    @Override
    public BigDecimal apply(BigDecimal price, BookingContext ctx) {
        if (ctx.getDaysUntilTour() >= MIN_DAYS_AHEAD) {
            return price.multiply(BigDecimal.valueOf(DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String getName() { return "Раннє бронювання (-10%)"; }
}
