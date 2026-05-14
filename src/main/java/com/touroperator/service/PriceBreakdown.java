package com.touroperator.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PriceBreakdown {

    private final BigDecimal basePrice;
    private final BigDecimal dynamicSurcharge;
    private final List<AppliedDiscount> appliedDiscounts;
    private final BigDecimal totalDiscount;
    private final BigDecimal finalPrice;

    public PriceBreakdown(BigDecimal basePrice, BigDecimal dynamicSurcharge,
          List<AppliedDiscount> appliedDiscounts,
          BigDecimal totalDiscount, BigDecimal finalPrice) {
        this.basePrice = basePrice;
        this.dynamicSurcharge = dynamicSurcharge;
        this.appliedDiscounts = Collections.unmodifiableList(new ArrayList<>(appliedDiscounts));
        this.totalDiscount = totalDiscount;
        this.finalPrice = finalPrice;
    }

    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getDynamicSurcharge() { return dynamicSurcharge; }
    public List<AppliedDiscount> getAppliedDiscounts() { return appliedDiscounts; }
    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public BigDecimal getFinalPrice() { return finalPrice; }


    public boolean hasDynamicSurcharge() {
        return dynamicSurcharge.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Розбивка ціни ===\n");
        sb.append(String.format("Базова ціна:     ₴%.2f%n", basePrice));
        if (hasDynamicSurcharge()) {
            sb.append(String.format("Динамічна надбавка: +₴%.2f%n", dynamicSurcharge));
        }
        for (AppliedDiscount d : appliedDiscounts) {
            sb.append(String.format("%-30s -₴%.2f%n", d.getName(), d.getAmount()));
        }
        sb.append(String.format("Знижка загалом:  -₴%.2f%n", totalDiscount));
        sb.append(String.format("РАЗОМ:           ₴%.2f%n", finalPrice));
        return sb.toString();
    }


    public static class AppliedDiscount {
        private final String name;
        private final BigDecimal amount;

        public AppliedDiscount(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public BigDecimal getAmount() { return amount; }
    }
}
