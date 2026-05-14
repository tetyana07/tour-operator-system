package com.touroperator.service.discount;

import java.math.BigDecimal;


public class GroupDiscount implements DiscountStrategy {

    private static final int MIN_GROUP_SIZE = 5;
    private static final double DISCOUNT_RATE = 0.07;

    @Override
    public BigDecimal apply(BigDecimal price, BookingContext ctx) {
        if (ctx.getTouristCount() >= MIN_GROUP_SIZE) {
            return price.multiply(BigDecimal.valueOf(DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String getName() { return "Групова знижка (-7%)"; }
}
