package com.touroperator.service.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class ExtraDiscount implements DiscountStrategy {

    private final int percent;

    public ExtraDiscount(int percent) {
        this.percent = percent;
    }

    @Override
    public BigDecimal apply(BigDecimal price, BookingContext ctx) {
        return price
              .multiply(BigDecimal.valueOf(percent))
              .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getName() {
        return "Додаткова знижка (-" + percent + "%)";
    }
}
