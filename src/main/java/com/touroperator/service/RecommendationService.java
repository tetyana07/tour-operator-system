package com.touroperator.service;

import com.touroperator.domain.Booking;
import com.touroperator.domain.Tour;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.TourRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

     
    private static final int SCORE_COUNTRY    = 40;
    private static final int SCORE_SEASON     = 25;
    private static final int SCORE_BUDGET     = 20;
    private static final int SCORE_DURATION   = 15;
    private static final int SCORE_POPULARITY = 10;

    private final BookingRepository bookingRepo;
    private final TourRepository    tourRepo;

    public RecommendationService(BookingRepository bookingRepo, TourRepository tourRepo) {
        this.bookingRepo = bookingRepo;
        this.tourRepo    = tourRepo;
    }

    /**
     * Повертає до {@code limit} рекомендованих турів для клієнта.
     *
     * @param clientId UUID клієнта
     * @param limit    максимальна кількість результатів
     * @return відсортований список рекомендацій (найрелевантніші першими)
     */
    public List<Tour> getRecommendations(UUID clientId, int limit) {
        log.debug("Рекомендації для клієнта {}", clientId);

        List<Booking> history = bookingRepo.findByClientId(clientId).stream()
              .filter(b -> "COMPLETED".equals(b.getStatus()) || "PAID".equals(b.getStatus()))
              .collect(Collectors.toList());

        List<Tour> allTours = tourRepo.findAll();

        if (history.isEmpty()) {
            log.debug("Новий клієнт — повертаємо найпопулярніші тури");
            return getPopular(allTours, limit);
        }

         
        ClientProfile profile = buildProfile(history, allTours);

         
        Set<UUID> bookedTourIds = history.stream()
              .map(Booking::getTourId)
              .collect(Collectors.toSet());

        List<Tour> candidates = allTours.stream()
              .filter(t -> !bookedTourIds.contains(t.getId()))
              .filter(t -> t.getAvailableSeats() > 0)
              .filter(t -> t.getStartDate() != null && t.getStartDate().isAfter(LocalDate.now()))
              .collect(Collectors.toList());

         
        List<Tour> result = candidates.stream()
              .sorted(Comparator.comparingInt((Tour t) -> score(t, profile)).reversed())
              .limit(limit)
              .collect(Collectors.toList());

         
        if (result.size() < limit) {
            getPopular(allTours, limit).stream()
                  .filter(t -> !result.contains(t))
                  .limit(limit - result.size())
                  .forEach(result::add);
        }

        log.info("Знайдено {} рекомендацій для клієнта {} (скоринг)", result.size(), clientId);
        return result;
    }

    /**
     * Повертає топ-N найпопулярніших доступних турів (без персоналізації).
     */
    public List<Tour> getPopular(List<Tour> tours, int limit) {
        return tours.stream()
              .filter(t -> t.getAvailableSeats() > 0)
              .filter(t -> t.getStartDate() != null && t.getStartDate().isAfter(LocalDate.now()))
              .sorted(Comparator.comparingDouble(Tour::getFillRate).reversed())
              .limit(limit)
              .collect(Collectors.toList());
    }

     

    private int score(Tour tour, ClientProfile p) {
        int total = 0;

         
        if (p.visitedCountries.contains(tour.getCountry())) {
            total += SCORE_COUNTRY;
        }

         
        if (tour.getStartDate() != null
              && p.preferredMonths.contains(tour.getStartDate().getMonthValue())) {
            total += SCORE_SEASON;
        }

         
        if (tour.getBasePrice() != null
              && tour.getBasePrice().compareTo(p.budgetMin) >= 0
              && tour.getBasePrice().compareTo(p.budgetMax) <= 0) {
            total += SCORE_BUDGET;
        }

         
        if (tour.getStartDate() != null && tour.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                  tour.getStartDate(), tour.getEndDate());
            if (Math.abs(days - p.avgDurationDays) <= 2) {
                total += SCORE_DURATION;
            }
        }

         
        total += (int) (tour.getFillRate() * SCORE_POPULARITY);

        return total;
    }

     

    private ClientProfile buildProfile(List<Booking> history, List<Tour> allTours) {
        Map<UUID, Tour> tourMap = allTours.stream()
              .collect(Collectors.toMap(Tour::getId, t -> t));

        List<Tour> visitedTours = history.stream()
              .map(b -> tourMap.get(b.getTourId()))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

         
        Set<String> countries = visitedTours.stream()
              .map(Tour::getCountry)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());

         
        Set<Integer> months = visitedTours.stream()
              .map(Tour::getStartDate)
              .filter(Objects::nonNull)
              .map(d -> d.getMonthValue())
              .collect(Collectors.toSet());

         
        BigDecimal avgPrice = history.stream()
              .map(Booking::getTotalPrice)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal budgetMin = avgPrice.multiply(BigDecimal.valueOf(0.70));
        BigDecimal budgetMax = avgPrice.multiply(BigDecimal.valueOf(1.30));

         
        double avgDays = visitedTours.stream()
              .filter(t -> t.getStartDate() != null && t.getEndDate() != null)
              .mapToLong(t -> java.time.temporal.ChronoUnit.DAYS.between(
                    t.getStartDate(), t.getEndDate()))
              .average()
              .orElse(7.0);

        log.debug("Профіль клієнта: країни={}, місяці={}, бюджет={}–{}, днів≈{}",
              countries, months, budgetMin, budgetMax, (long) avgDays);

        return new ClientProfile(countries, months, budgetMin, budgetMax, (long) avgDays);
    }

     

    private record ClientProfile(
          Set<String>  visitedCountries,
          Set<Integer> preferredMonths,
          BigDecimal   budgetMin,
          BigDecimal   budgetMax,
          long         avgDurationDays
    ) {}
}