package com.touroperator.domain;

import java.time.LocalDate;
import java.util.UUID;


public class PromoCode {
    private UUID id;
    private String code;
    private int discountPercent;
    private LocalDate validUntil;

    public PromoCode() {}

    public PromoCode(String code, int discountPercent, LocalDate validUntil) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.validUntil = validUntil;
    }

    public PromoCode(UUID id, String code, int discountPercent, LocalDate validUntil) {
        this.id = id;
        this.code = code;
        this.discountPercent = discountPercent;
        this.validUntil = validUntil;
    }


    public boolean isValid() {
        return validUntil != null && !validUntil.isBefore(LocalDate.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    @Override
    public String toString() {
        return String.format("PromoCode[code='%s', discount=%d%%, until=%s, valid=%s]",
              code, discountPercent, validUntil, isValid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromoCode p = (PromoCode) o;
        return id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }
}
