package com.touroperator.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;


public class CurrencyRate {

    private BigDecimal usdToUah;
    private BigDecimal usdToEur;
    private LocalDateTime fetchedAt;
    private boolean fromCache;

    public CurrencyRate() {}

    public CurrencyRate(BigDecimal usdToUah, BigDecimal usdToEur,
          LocalDateTime fetchedAt, boolean fromCache) {
        this.usdToUah = usdToUah;
        this.usdToEur = usdToEur;
        this.fetchedAt = fetchedAt;
        this.fromCache = fromCache;
    }


    public BigDecimal toUah(BigDecimal usd) {
        if (usdToUah == null || usd == null) return BigDecimal.ZERO;
        return usd.multiply(usdToUah).setScale(2, RoundingMode.HALF_UP);
    }


    public BigDecimal toEur(BigDecimal usd) {
        if (usdToEur == null || usd == null) return BigDecimal.ZERO;
        return usd.multiply(usdToEur).setScale(2, RoundingMode.HALF_UP);
    }


    public static CurrencyRate fallback() {
        return new CurrencyRate(
              new BigDecimal("41.50"),
              new BigDecimal("0.93"),
              LocalDateTime.now(),
              true
        );
    }

    public BigDecimal getUsdToUah() { return usdToUah; }
    public void setUsdToUah(BigDecimal usdToUah) { this.usdToUah = usdToUah; }

    public BigDecimal getUsdToEur() { return usdToEur; }
    public void setUsdToEur(BigDecimal usdToEur) { this.usdToEur = usdToEur; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }

    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }

    @Override
    public String toString() {
        return String.format("CurrencyRate[1 USD = %.2f UAH, 1 USD = %.4f EUR, cache=%s]",
              usdToUah, usdToEur, fromCache);
    }
}
