package com.touroperator.service;

import com.touroperator.domain.Booking;
import com.touroperator.domain.Tour;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.TourRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final BookingRepository bookingRepo;
    private final TourRepository tourRepo;

    @Autowired
    public RecommendationService(BookingRepository bookingRepo, TourRepository tourRepo) {
        this.bookingRepo = bookingRepo;
        this.tourRepo = tourRepo;
    }

    public List<Tour> getRecommendations(UUID clientId, int limit) {
        log.debug("Рекомендації для клієнта {}", clientId);

        List<Booking> history = bookingRepo.findAll().stream()
              .filter(b -> clientId.equals(b.getClientId()))
              .filter(b -> "COMPLETED".equals(b.getStatus()) || "PAID".equals(b.getStatus()))
              .collect(Collectors.toList());

        List<Tour> allTours = tourRepo.findAll();

        if (history.isEmpty()) {
            log.debug("Новий клієнт — повертаємо найпопулярніші тури");
            return getMostPopular(allTours, limit);
        }


        Set<UUID> visitedTourIds = history.stream()
              .map(Booking::getTourId)
              .collect(Collectors.toSet());


        BigDecimal avgBudget = history.stream()
              .map(Booking::getTotalPrice)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal minBudget = avgBudget.multiply(BigDecimal.valueOf(0.7));
        BigDecimal maxBudget = avgBudget.multiply(BigDecimal.valueOf(1.3));

        log.debug("Бюджет клієнта: ₴{} (діапазон ₴{}–₴{})", avgBudget, minBudget, maxBudget);

        List<Tour> recommendations = allTours.stream()
              .filter(t -> !visitedTourIds.contains(t.getId())) // нові тури
              .filter(t -> t.getAvailableSeats() > 0)           // є місця
              .filter(t -> t.getBasePrice().compareTo(minBudget) >= 0
                    && t.getBasePrice().compareTo(maxBudget) <= 0) // схожий бюджет
              .sorted(Comparator.comparingDouble(Tour::getFillRate).reversed()) // популярніші
              .limit(limit)
              .collect(Collectors.toList());


        if (recommendations.size() < limit) {
            getMostPopular(allTours, limit).stream()
                  .filter(t -> !recommendations.contains(t))
                  .limit(limit - recommendations.size())
                  .forEach(recommendations::add);
        }

        log.info("Знайдено {} рекомендацій для клієнта {}", recommendations.size(), clientId);
        return recommendations;
    }

    private List<Tour> getMostPopular(List<Tour> tours, int limit) {
        return tours.stream()
              .filter(t -> t.getAvailableSeats() > 0)
              .sorted(Comparator.comparingDouble(Tour::getFillRate).reversed())
              .limit(limit)
              .collect(Collectors.toList());
    }
}
