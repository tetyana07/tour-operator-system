package com.touroperator.service;

import com.touroperator.domain.*;
import com.touroperator.exception.PromoCodeExpiredException;
import com.touroperator.repository.*;
import com.touroperator.service.discount.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
public class PricingService {

    private final ExcursionRepository excursionRepo;
    private final InsuranceRepository insuranceRepo;
    private final TransferRepository  transferRepo;
    private final PromoCodeRepository promoRepo;

    public PricingService(ExcursionRepository excursionRepo,
          InsuranceRepository insuranceRepo,
          TransferRepository  transferRepo,
          PromoCodeRepository promoRepo) {
        this.excursionRepo = excursionRepo;
        this.insuranceRepo = insuranceRepo;
        this.transferRepo  = transferRepo;
        this.promoRepo     = promoRepo;
    }


       

    public PriceBreakdown calculate(Tour tour, int adults, int children, PromoCode promoCode) {
        BookingContext ctx = new BookingContext(tour, adults, children, promoCode);

        BigDecimal base = tour.getBasePrice()
              .multiply(BigDecimal.valueOf((long) adults + children))
              .setScale(2, RoundingMode.HALF_UP);

        BigDecimal afterDynamic = applyDynamic(base, tour.getFillRate());
        BigDecimal dynamicSurcharge = afterDynamic.subtract(base);

        List<DiscountStrategy> strategies = buildStrategies(ctx);
        return applyStrategies(base, dynamicSurcharge, afterDynamic, strategies, ctx);
    }

    public PriceBreakdown calculate(Tour tour,
          int touristCount,
          List<UUID> excursionIds,
          UUID insuranceId,
          UUID transferId,
          String promoCode) {
        return calculate(tour, touristCount, 0, excursionIds, insuranceId, transferId, promoCode, 0);
    }

    public PriceBreakdown calculate(Tour tour,
          int touristCount,
          List<UUID> excursionIds,
          UUID insuranceId,
          UUID transferId,
          String promoCode,
          int extraDiscountPercent) {
        return calculate(tour, touristCount, 0, excursionIds, insuranceId, transferId, promoCode, extraDiscountPercent);
    }

    public PriceBreakdown calculate(Tour tour,
          int touristCount,
          int childCount,
          List<UUID> excursionIds,
          UUID insuranceId,
          UUID transferId,
          String promoCode,
          int extraDiscountPercent) {

        PromoCode pc = resolvePromoCode(promoCode);
        BookingContext ctx = new BookingContext(tour, touristCount, childCount, pc);

        BigDecimal base = tour.getBasePrice()
              .multiply(BigDecimal.valueOf(touristCount))
              .setScale(2, RoundingMode.HALF_UP);

        BigDecimal extras = extrasTotal(excursionIds, insuranceId, transferId);
        BigDecimal subtotal = base.add(extras);

        BigDecimal afterDynamic = applyDynamic(subtotal, tour.getFillRate());
        BigDecimal dynamicSurcharge = afterDynamic.subtract(subtotal);

        List<DiscountStrategy> strategies = buildStrategies(ctx);
        if (extraDiscountPercent > 0) {
            strategies.add(new ExtraDiscount(extraDiscountPercent));
        }

        return applyStrategies(base, dynamicSurcharge, afterDynamic, strategies, ctx);
    }


       

    private List<DiscountStrategy> buildStrategies(BookingContext ctx) {
        List<DiscountStrategy> strategies = new ArrayList<>();
        strategies.add(new ChildDiscount());
        strategies.add(new EarlyBookingDiscount());
        strategies.add(new GroupDiscount());
        if (ctx.getPromoCode() != null) {
            strategies.add(new PromoCodeDiscount(ctx.getPromoCode()));
        }
        return strategies;
    }

    private PriceBreakdown applyStrategies(BigDecimal base,
          BigDecimal dynamicSurcharge,
          BigDecimal priceBeforeDiscounts,
          List<DiscountStrategy> strategies,
          BookingContext ctx) {

        List<PriceBreakdown.AppliedDiscount> applied = new ArrayList<>();
        BigDecimal running = priceBeforeDiscounts;

        for (DiscountStrategy strategy : strategies) {
            BigDecimal amount = strategy.apply(running, ctx)
                  .setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                applied.add(new PriceBreakdown.AppliedDiscount(strategy.getName(), amount));
                running = running.subtract(amount);
            }
        }

        BigDecimal totalDiscount = applied.stream()
              .map(PriceBreakdown.AppliedDiscount::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalPrice = running.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PriceBreakdown(base, dynamicSurcharge.max(BigDecimal.ZERO),
              applied, totalDiscount, finalPrice);
    }

    private BigDecimal applyDynamic(BigDecimal price, double fillRate) {
        return price.multiply(dynamicMultiplier(fillRate)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal extrasTotal(List<UUID> excursionIds, UUID insuranceId, UUID transferId) {
        BigDecimal total = BigDecimal.ZERO;
        if (excursionIds != null) {
            for (UUID id : excursionIds) {
                var exc = excursionRepo.findById(id);
                if (exc.isPresent()) total = total.add(exc.get().getPrice());
            }
        }
        if (insuranceId != null) {
            total = total.add(insuranceRepo.findById(insuranceId)
                  .map(Insurance::getPrice).orElse(BigDecimal.ZERO));
        }
        if (transferId != null) {
            total = total.add(transferRepo.findById(transferId)
                  .map(Transfer::getPrice).orElse(BigDecimal.ZERO));
        }
        return total;
    }

    private PromoCode resolvePromoCode(String code) {
        if (code == null || code.isBlank()) return null;
        PromoCode pc = promoRepo.findByCode(code.trim()).orElse(null);
        if (pc != null && !pc.isValid()) {
            throw new PromoCodeExpiredException(
                  "Промокод '" + pc.getCode() + "' прострочений (дійсний до " + pc.getValidUntil() + ")");
        }
        return pc;
    }

    public BigDecimal dynamicMultiplier(double fillRate) {
        if (fillRate > 0.90) return new BigDecimal("1.25");
        if (fillRate > 0.70) return new BigDecimal("1.15");
        if (fillRate > 0.50) return new BigDecimal("1.05");
        return BigDecimal.ONE;
    }
}
