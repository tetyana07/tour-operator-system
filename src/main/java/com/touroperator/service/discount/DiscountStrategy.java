package com.touroperator.service.discount;

import java.math.BigDecimal;


public interface DiscountStrategy {

    BigDecimal apply(BigDecimal price, BookingContext ctx);


    String getName();
}
