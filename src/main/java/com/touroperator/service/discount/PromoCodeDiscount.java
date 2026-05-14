package com.touroperator.service.discount;

import com.touroperator.domain.PromoCode;
import com.touroperator.exception.PromoCodeExpiredException;

import java.math.BigDecimal;


public class PromoCodeDiscount implements DiscountStrategy {

    private final PromoCode promoCode;

    public PromoCodeDiscount(PromoCode promoCode) {
        this.promoCode = promoCode;
    }

    @Override
    public BigDecimal apply(BigDecimal price, BookingContext ctx) {
        if (!promoCode.isValid()) {
            throw new PromoCodeExpiredException(promoCode.getCode());
        }
        return price.multiply(
              BigDecimal.valueOf(promoCode.getDiscountPercent() / 100.0)
        );
    }

    @Override
    public String getName() {
        return "Промокод " + promoCode.getCode() +
              " (-" + promoCode.getDiscountPercent() + "%)";
    }
}
