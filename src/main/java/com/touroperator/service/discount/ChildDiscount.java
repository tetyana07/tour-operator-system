package com.touroperator.service.discount;

import java.math.BigDecimal;

public class ChildDiscount implements DiscountStrategy {

    private static final double CHILD_DISCOUNT_RATE = 0.30;

    @Override
    public BigDecimal apply(BigDecimal price, BookingContext ctx) {
        if (ctx.getChildCount() <= 0) return BigDecimal.ZERO;

        BigDecimal pricePerPerson = ctx.getTouristCount() > 0
              ? price.divide(BigDecimal.valueOf(ctx.getTouristCount()), 2, java.math.RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
        return pricePerPerson
              .multiply(BigDecimal.valueOf(CHILD_DISCOUNT_RATE))
              .multiply(BigDecimal.valueOf(ctx.getChildCount()));
    }

    @Override
    public String getName() {
        return "Дитяча знижка (-30% × " + "дітей)";
    }
}
