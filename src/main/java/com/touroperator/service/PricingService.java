package com.touroperator.service;

import com.touroperator.domain.*;
import com.touroperator.exception.PromoCodeExpiredException;
import com.touroperator.repository.*;
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
        int total = adults + children;
        List<PriceBreakdown.AppliedDiscount> discounts = new ArrayList<>();

        BigDecimal base = tour.getBasePrice()
              .multiply(BigDecimal.valueOf(total))
              .setScale(2, RoundingMode.HALF_UP);


        BigDecimal childDiscount = BigDecimal.ZERO;
        if (children > 0) {
            childDiscount = tour.getBasePrice()
                  .multiply(BigDecimal.valueOf(children))
                  .multiply(new BigDecimal("0.50"))
                  .setScale(2, RoundingMode.HALF_UP);
            discounts.add(new PriceBreakdown.AppliedDiscount(
                  "Дитяча знижка (×" + children + ")", childDiscount));
        }

        BigDecimal multiplier = dynamicMultiplier(tour.getFillRate());
        BigDecimal afterDynamic = base.subtract(childDiscount)
              .multiply(multiplier)
              .setScale(2, RoundingMode.HALF_UP);
        BigDecimal dynamicSurcharge = afterDynamic.subtract(base.subtract(childDiscount));

        BigDecimal promoDiscount = BigDecimal.ZERO;
        if (promoCode != null) {
            if (!promoCode.isValid()) {
                throw new PromoCodeExpiredException(
                      "Промокод '" + promoCode.getCode() + "' прострочений");
            }
            promoDiscount = afterDynamic
                  .multiply(BigDecimal.valueOf(promoCode.getDiscountPercent()))
                  .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            discounts.add(new PriceBreakdown.AppliedDiscount(
                  "Промокод " + promoCode.getCode() + " (−" + promoCode.getDiscountPercent() + "%)",
                  promoDiscount));
        }

        BigDecimal totalDiscount = discounts.stream()
              .map(PriceBreakdown.AppliedDiscount::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalPrice = afterDynamic.subtract(promoDiscount)
              .max(BigDecimal.ZERO)
              .setScale(2, RoundingMode.HALF_UP);

        return new PriceBreakdown(base, dynamicSurcharge, discounts, totalDiscount, finalPrice);
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
        List<PriceBreakdown.AppliedDiscount> discounts = new ArrayList<>();

        BigDecimal base = tour.getBasePrice()
              .multiply(BigDecimal.valueOf(touristCount))
              .setScale(2, RoundingMode.HALF_UP);

        BigDecimal childDiscount = BigDecimal.ZERO;
        if (childCount > 0) {
            childDiscount = tour.getBasePrice()
                  .multiply(BigDecimal.valueOf(childCount))
                  .multiply(new BigDecimal("0.50"))
                  .setScale(2, RoundingMode.HALF_UP);
            discounts.add(new PriceBreakdown.AppliedDiscount(
                  "Дитяча знижка (×" + childCount + ")", childDiscount));
        }

        BigDecimal excTotal = BigDecimal.ZERO;
        if (excursionIds != null) {
            for (UUID id : excursionIds) {
                var exc = excursionRepo.findById(id);
                if (exc.isPresent()) excTotal = excTotal.add(exc.get().getPrice());
            }
        }

        BigDecimal insPrice = BigDecimal.ZERO;
        if (insuranceId != null) {
            insPrice = insuranceRepo.findById(insuranceId)
                  .map(Insurance::getPrice).orElse(BigDecimal.ZERO);
        }

        BigDecimal tranPrice = BigDecimal.ZERO;
        if (transferId != null) {
            tranPrice = transferRepo.findById(transferId)
                  .map(Transfer::getPrice).orElse(BigDecimal.ZERO);
        }

        BigDecimal subtotal = base.subtract(childDiscount).add(excTotal).add(insPrice).add(tranPrice);

        BigDecimal multiplier = dynamicMultiplier(tour.getFillRate());
        BigDecimal afterDynamic = subtotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dynamicSurcharge = afterDynamic.subtract(subtotal);
        if (dynamicSurcharge.compareTo(BigDecimal.ZERO) > 0) {
            discounts.add(new PriceBreakdown.AppliedDiscount(
                  "Динамічна надбавка (×" + multiplier + ")", dynamicSurcharge.negate()));
        }

        BigDecimal promoDiscountAmt = BigDecimal.ZERO;
        if (promoCode != null && !promoCode.isBlank()) {
            PromoCode pc = promoRepo.findByCode(promoCode.trim()).orElse(null);
            if (pc != null) {
                if (!pc.isValid()) {
                    throw new PromoCodeExpiredException(
                          "Промокод '" + pc.getCode() + "' прострочений (до " + pc.getValidUntil() + ")");
                }
                promoDiscountAmt = afterDynamic
                      .multiply(BigDecimal.valueOf(pc.getDiscountPercent()))
                      .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                discounts.add(new PriceBreakdown.AppliedDiscount(
                      "Промокод " + pc.getCode() + " (−" + pc.getDiscountPercent() + "%)",
                      promoDiscountAmt));
            }
        }


        BigDecimal totalDiscount = childDiscount.add(promoDiscountAmt);
        BigDecimal afterPromo = afterDynamic.subtract(promoDiscountAmt);


        BigDecimal extraDiscountAmt = BigDecimal.ZERO;
        if (extraDiscountPercent > 0) {
            extraDiscountAmt = afterPromo
                  .multiply(BigDecimal.valueOf(extraDiscountPercent))
                  .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            discounts.add(new PriceBreakdown.AppliedDiscount(
                  "Додаткова знижка (−" + extraDiscountPercent + "%)", extraDiscountAmt));
            totalDiscount = totalDiscount.add(extraDiscountAmt);
        }

        BigDecimal finalPrice = afterPromo.subtract(extraDiscountAmt)
              .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new PriceBreakdown(base, dynamicSurcharge.max(BigDecimal.ZERO),
              discounts, totalDiscount, finalPrice);
    }

    public BigDecimal dynamicMultiplier(double fillRate) {
        if (fillRate > 0.90) return new BigDecimal("1.25");
        if (fillRate > 0.70) return new BigDecimal("1.15");
        if (fillRate > 0.50) return new BigDecimal("1.05");
        return BigDecimal.ONE;
    }
}