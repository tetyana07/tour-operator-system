package com.touroperator.service;

import com.touroperator.domain.Booking;
import com.touroperator.domain.Tour;
import com.touroperator.dto.TourReport;
import com.touroperator.domain.BookingStatus;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.TourRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ReportService {

    private final TourRepository    tourRepo;
    private final BookingRepository bookingRepo;

    public ReportService(TourRepository tourRepo, BookingRepository bookingRepo) {
        this.tourRepo    = tourRepo;
        this.bookingRepo = bookingRepo;
    }

    public List<TourReport> getTourReports() {
        List<Tour> tours = tourRepo.findAll();
        List<TourReport> reports = new ArrayList<>();

        for (Tour tour : tours) {
            List<Booking> bookings = bookingRepo.findByTourId(tour.getId());

            int totalBookings = bookings.size();
            int totalTourists = bookings.stream()
                  .mapToInt(Booking::getTouristCount)
                  .sum();

            BigDecimal revenue = bookings.stream()
                  .filter(b -> BookingStatus.PAID.name().equals(b.getStatus())
                        || BookingStatus.COMPLETED.name().equals(b.getStatus()))
                  .map(Booking::getTotalPrice)
                  .filter(Objects::nonNull)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

            double fillPercent = tour.getFillRate() * 100;

            reports.add(new TourReport(
                  tour.getName(),
                  totalBookings,
                  totalTourists,
                  revenue,
                  fillPercent
            ));
        }

        reports.sort(Comparator.comparing(TourReport::getTotalRevenue).reversed());
        return reports;
    }

    public BigDecimal getTotalRevenue() {
        return bookingRepo.findByStatus(BookingStatus.PAID).stream()
              .map(Booking::getTotalPrice)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .add(
                    bookingRepo.findByStatus(BookingStatus.COMPLETED).stream()
                          .map(Booking::getTotalPrice)
                          .filter(Objects::nonNull)
                          .reduce(BigDecimal.ZERO, BigDecimal::add)
              );
    }

    public List<TourReport> getTopTours(int limit) {
        return getTourReports().stream()
              .sorted(Comparator.comparingInt(TourReport::getTotalBookings).reversed())
              .limit(limit)
              .collect(Collectors.toList());
    }

    public Map<String, Long> getBookingStatusStats() {
        return bookingRepo.findAll().stream()
              .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
    }

    public long getActiveBookingsCount() {
        return bookingRepo.findByStatus(BookingStatus.CONFIRMED).size()
              + bookingRepo.findByStatus(BookingStatus.CREATED).size();
    }

    public LinkedHashMap<String, Integer> getTopCountries(int limit) {
        List<Booking> allBookings = bookingRepo.findAll().stream()
              .filter(b -> !BookingStatus.CANCELLED.name().equals(b.getStatus()))
              .collect(Collectors.toList());

        if (allBookings.isEmpty()) return new LinkedHashMap<>();

        Map<String, Long> countByCountry = new LinkedHashMap<>();
        for (Booking booking : allBookings) {
            try {
                Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
                if (tour != null && tour.getCountry() != null && !tour.getCountry().isBlank()) {
                    countByCountry.merge(tour.getCountry(), 1L, Long::sum);
                }
            } catch (Exception ignored) {}
        }

        long total = countByCountry.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return new LinkedHashMap<>();

        List<Map.Entry<String, Long>> sorted = countByCountry.entrySet().stream()
              .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
              .collect(Collectors.toList());

        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        long othersCount = 0;
        int i = 0;
        for (Map.Entry<String, Long> entry : sorted) {
            if (i < limit) {
                int pct = (int) Math.round(entry.getValue() * 100.0 / total);
                result.put(entry.getKey(), pct);
                i++;
            } else {
                othersCount += entry.getValue();
            }
        }
        if (othersCount > 0) {
            int pct = (int) Math.round(othersCount * 100.0 / total);
            result.put("Інші", pct);
        }
        return result;
    }

    public List<Tour> getCriticallyFilledTours() {
        return tourRepo.findActive().stream()
              .filter(t -> t.getFillRate() > 0.80)
              .sorted(Comparator.comparingDouble(Tour::getFillRate).reversed())
              .collect(Collectors.toList());
    }
}