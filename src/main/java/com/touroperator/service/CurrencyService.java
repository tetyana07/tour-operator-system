package com.touroperator.service;

import com.touroperator.dto.CurrencyRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;


@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);


    private static final String API_URL =
          "https://open.er-api.com/v6/latest/USD";

    private final RestTemplate restTemplate;


    private CurrencyRate cachedRate;
    private LocalDateTime cacheTime;
    private static final int CACHE_HOURS = 1;

    public CurrencyService() {
        this.restTemplate = new RestTemplate();
    }


    public CurrencyRate getCurrentRates() {
        if (cachedRate != null && cacheTime != null &&
              cacheTime.plusHours(CACHE_HOURS).isAfter(LocalDateTime.now())) {
            log.debug("Курс валют з кешу: {}", cachedRate);
            return cachedRate;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);

            if (response != null && "success".equals(response.get("result"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");

                double uah = toDouble(rates.get("UAH"));
                double eur = toDouble(rates.get("EUR"));

                cachedRate = new CurrencyRate(
                      BigDecimal.valueOf(uah),
                      BigDecimal.valueOf(eur),
                      LocalDateTime.now(),
                      false
                );
                cacheTime = LocalDateTime.now();
                log.info("Курс валют оновлено: {}", cachedRate);
                return cachedRate;
            }
        } catch (Exception e) {
            log.warn("API курсів валют недоступний: {}. Використовуємо fallback.", e.getMessage());
        }

        log.info("Офлайн-режим: використовуємо стандартний курс");
        cachedRate = CurrencyRate.fallback();
        cacheTime  = LocalDateTime.now();
        return cachedRate;
    }

    public String formatMultiCurrency(BigDecimal usd) {
        CurrencyRate rate = getCurrentRates();
        return String.format("$%.2f USD  ≈  ₴%.0f UAH  ≈  €%.2f EUR",
              usd,
              rate.toUah(usd),
              rate.toEur(usd));
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(val.toString());
    }
}
