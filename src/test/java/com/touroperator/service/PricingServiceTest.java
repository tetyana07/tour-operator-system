package com.touroperator.service;

import com.touroperator.service.PriceBreakdown;
import com.touroperator.domain.PromoCode;
import com.touroperator.domain.Tour;
import com.touroperator.repository.ExcursionRepository;
import com.touroperator.repository.InsuranceRepository;
import com.touroperator.repository.PromoCodeRepository;
import com.touroperator.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PricingService — розрахунок ціни")
class PricingServiceTest {

    @Mock private ExcursionRepository excursionRepo;
    @Mock private InsuranceRepository insuranceRepo;
    @Mock private TransferRepository  transferRepo;
    @Mock private PromoCodeRepository promoRepo;

    @InjectMocks
    private PricingService pricingService;

    private Tour tourNear;    // старт через 10 днів — без знижки раннього бронювання
    private Tour tourFuture;  // старт через 60 днів — з ранньою знижкою

    @BeforeEach
    void setUp() {
        tourNear   = buildTour(new BigDecimal("1000.00"), 10, 2);
        tourFuture = buildTour(new BigDecimal("1000.00"), 10, 2);
        tourFuture.setStartDate(LocalDate.now().plusDays(60));
    }

    // ── Динамічне ціноутворення ───────────────────────────────────────────

    @Nested
    @DisplayName("dynamicMultiplier()")
    class DynamicMultiplierTests {

        @Test
        @DisplayName("fillRate ≤ 50% → множник 1.0")
        void lowFill_noSurcharge() {
            // BigDecimal.ONE.stripTrailingZeros() → "1", а не "1.00"
            // Порівнюємо через compareTo щоб уникнути проблеми scale
            assertEquals(0,
                  BigDecimal.ONE.compareTo(pricingService.dynamicMultiplier(0.50)),
                  "Очікується множник 1.0 при fillRate ≤ 50%");
        }

        @Test
        @DisplayName("fillRate 51–70% → множник 1.05")
        void mediumFill_surcharge5pct() {
            assertEquals(new BigDecimal("1.05"),
                  pricingService.dynamicMultiplier(0.65));
        }

        @Test
        @DisplayName("fillRate 71–90% → множник 1.15")
        void highFill_surcharge15pct() {
            assertEquals(new BigDecimal("1.15"),
                  pricingService.dynamicMultiplier(0.80));
        }

        @Test
        @DisplayName("fillRate > 90% → множник 1.25")
        void nearFull_surcharge25pct() {
            assertEquals(new BigDecimal("1.25"),
                  pricingService.dynamicMultiplier(0.95));
        }
    }

    // ── Базовий розрахунок без знижок ─────────────────────────────────────

    @Test
    @DisplayName("1 турист, ціна 1000 → finalPrice = 1000")
    void singleAdult_noDiscount() {
        PriceBreakdown result = calc(tourNear, 1, 0, null);

        assertEquals(new BigDecimal("1000.00"), result.getBasePrice());
        assertEquals(new BigDecimal("1000.00"), result.getFinalPrice());
    }

    @Test
    @DisplayName("2 туристи, ціна 1000 → base = 2000")
    void twoAdults_baseDoubled() {
        PriceBreakdown result = calc(tourNear, 2, 0, null);

        assertEquals(new BigDecimal("2000.00"), result.getBasePrice());
    }

    // ── Дитяча знижка ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ChildDiscount (-30% на дитину)")
    class ChildDiscountTests {

        @Test
        @DisplayName("1 дорослий + 1 дитина: знижка = 450 (30% від ціни на дитину з бази 3000/2 осіб)")
        void oneChild_discount30pct() {
            // adults=2 (1 дор + 1 дитина передається як adults), children=1
            // base = 1000 * (2 + 1) = 3000
            // ctx.touristCount = adults = 2
            // pricePerPerson = 3000 / 2 = 1500
            // childDiscount = 1500 * 0.30 * 1 = 450
            PriceBreakdown result = calc(tourNear, 2, 1, null);

            boolean hasChildDiscount = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Дитяча"));
            assertTrue(hasChildDiscount, "Має бути дитяча знижка");

            BigDecimal childDiscount = result.getAppliedDiscounts().stream()
                  .filter(d -> d.getName().contains("Дитяча"))
                  .map(PriceBreakdown.AppliedDiscount::getAmount)
                  .findFirst().orElse(BigDecimal.ZERO);

            assertEquals(new BigDecimal("450.00"), childDiscount);
        }

        @Test
        @DisplayName("0 дітей → дитяча знижка не застосовується")
        void noChildren_noDiscount() {
            PriceBreakdown result = calc(tourNear, 2, 0, null);

            boolean hasChildDiscount = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Дитяча"));
            assertFalse(hasChildDiscount);
        }
    }

    // ── Групова знижка ────────────────────────────────────────────────────

    @Nested
    @DisplayName("GroupDiscount (-7% при ≥5 туристів)")
    class GroupDiscountTests {

        @Test
        @DisplayName("5 туристів → групова знижка застосована")
        void fiveTourists_groupDiscountApplied() {
            PriceBreakdown result = calc(tourNear, 5, 0, null);

            boolean hasGroup = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Групова"));
            assertTrue(hasGroup, "При 5 туристах має бути групова знижка");
        }

        @Test
        @DisplayName("4 туристи → групова знижка НЕ застосовується")
        void fourTourists_noGroupDiscount() {
            PriceBreakdown result = calc(tourNear, 4, 0, null);

            boolean hasGroup = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Групова"));
            assertFalse(hasGroup);
        }

        @Test
        @DisplayName("5 туристів по 1000 → знижка 7% від 5000 = 350")
        void fiveTourists_discountAmount() {
            PriceBreakdown result = calc(tourNear, 5, 0, null);

            BigDecimal groupDiscount = result.getAppliedDiscounts().stream()
                  .filter(d -> d.getName().contains("Групова"))
                  .map(PriceBreakdown.AppliedDiscount::getAmount)
                  .findFirst().orElse(BigDecimal.ZERO);

            assertEquals(new BigDecimal("350.00"), groupDiscount);
        }
    }

    // ── Рання знижка ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("EarlyBookingDiscount (-10% при бронюванні за ≥30 днів)")
    class EarlyBookingDiscountTests {

        @Test
        @DisplayName("Тур через 60 днів → рання знижка -10%")
        void tourIn60Days_earlyDiscount() {
            PriceBreakdown result = calc(tourFuture, 1, 0, null);

            boolean hasEarly = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Раннє"));
            assertTrue(hasEarly, "Має бути знижка раннього бронювання");

            BigDecimal earlyDiscount = result.getAppliedDiscounts().stream()
                  .filter(d -> d.getName().contains("Раннє"))
                  .map(PriceBreakdown.AppliedDiscount::getAmount)
                  .findFirst().orElse(BigDecimal.ZERO);
            assertEquals(new BigDecimal("100.00"), earlyDiscount);
        }

        @Test
        @DisplayName("Тур через 10 днів → рання знижка НЕ застосовується")
        void tourIn10Days_noEarlyDiscount() {
            PriceBreakdown result = calc(tourNear, 1, 0, null);

            boolean hasEarly = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Раннє"));
            assertFalse(hasEarly);
        }
    }

    // ── Промокод ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PromoCodeDiscount")
    class PromoCodeDiscountTests {

        @Test
        @DisplayName("Валідний промокод 20% → знижка застосована")
        void validPromoCode_discountApplied() {
            PromoCode promo = new PromoCode(UUID.randomUUID(), "ЛІТО20", 20,
                  LocalDate.now().plusMonths(1));
            when(promoRepo.findByCode("ЛІТО20")).thenReturn(Optional.of(promo));

            PriceBreakdown result = pricingService.calculate(
                  tourNear, 1, 0,
                  Collections.emptyList(), null, null, "ЛІТО20", 0);

            boolean hasPromo = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("ЛІТО20") ||
                        d.getName().contains("Промокод"));
            assertTrue(hasPromo, "Має бути знижка промокоду");
        }

        @Test
        @DisplayName("Прострочений промокод → PromoCodeExpiredException")
        void expiredPromoCode_throwsException() {
            PromoCode expired = new PromoCode(UUID.randomUUID(), "СТАРИЙ", 10,
                  LocalDate.now().minusDays(1));
            when(promoRepo.findByCode("СТАРИЙ")).thenReturn(Optional.of(expired));

            assertThrows(
                  com.touroperator.exception.PromoCodeExpiredException.class,
                  () -> pricingService.calculate(
                        tourNear, 1, 0,
                        Collections.emptyList(), null, null, "СТАРИЙ", 0)
            );
        }

        @Test
        @DisplayName("Null промокод → ніякої знижки промокоду")
        void nullPromoCode_noPromoDiscount() {
            PriceBreakdown result = pricingService.calculate(
                  tourNear, 1, 0,
                  Collections.emptyList(), null, null, null, 0);

            boolean hasPromo = result.getAppliedDiscounts().stream()
                  .anyMatch(d -> d.getName().contains("Промокод"));
            assertFalse(hasPromo);
        }
    }

    // ── Фінальна ціна не від'ємна ─────────────────────────────────────────

    @Test
    @DisplayName("Сума знижок не перевищує ціну (finalPrice ≥ 0)")
    void finalPrice_neverNegative() {
        PromoCode bigPromo = new PromoCode(UUID.randomUUID(), "БЕЗКОШТОВНО", 100,
              LocalDate.now().plusMonths(1));
        when(promoRepo.findByCode("БЕЗКОШТОВНО")).thenReturn(Optional.of(bigPromo));

        PriceBreakdown result = pricingService.calculate(
              tourFuture, 5, 2,
              Collections.emptyList(), null, null, "БЕЗКОШТОВНО", 0);

        assertTrue(result.getFinalPrice().compareTo(BigDecimal.ZERO) >= 0,
              "Фінальна ціна не повинна бути від'ємною");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private PriceBreakdown calc(Tour tour, int adults, int children, PromoCode promo) {
        return pricingService.calculate(tour, adults, children, promo);
    }

    private Tour buildTour(BigDecimal price, int quota, int booked) {
        Tour t = new Tour();
        t.setId(UUID.randomUUID());
        t.setName("Тестовий тур");
        t.setCountry("Тестова країна");
        t.setCity("Місто");
        t.setBasePrice(price);
        t.setQuota(quota);
        t.setBookedSeats(booked);
        t.setStartDate(LocalDate.now().plusDays(10));
        t.setEndDate(LocalDate.now().plusDays(17));
        return t;
    }
}