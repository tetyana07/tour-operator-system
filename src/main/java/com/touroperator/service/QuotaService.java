package com.touroperator.service;

import com.touroperator.domain.Tour;
import com.touroperator.exception.EntityNotFoundException;
import com.touroperator.exception.QuotaExceededException;
import com.touroperator.repository.TourRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final TourRepository tourRepository;
    private final List<QuotaListener> listeners = new ArrayList<>();

    @Autowired
    public QuotaService(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    public void addListener(QuotaListener listener)    { listeners.add(listener); }
    public void removeListener(QuotaListener listener) { listeners.remove(listener); }

    public void reserveSeats(UUID tourId, int count) {
        Tour tour = findTourOrThrow(tourId);
        int available = tour.getAvailableSeats();
        if (count > available) {
            notifyExceeded(tour);
            throw new QuotaExceededException(count, available);
        }
        tourRepository.incrementBookedSeats(tourId, count);
        notifyChanged(tour, tour.getAvailableSeats() - count);
        log.info("Зарезервовано {} місць у '{}'", count, tour.getName());
    }

    public void releaseSeats(UUID tourId, int count) {
        Tour tour = findTourOrThrow(tourId);
        tourRepository.decrementBookedSeats(tourId, count);
        notifyChanged(tour, tour.getAvailableSeats() + count);
        log.info("Звільнено {} місць у '{}'", count, tour.getName());
    }

    public boolean hasAvailableSeats(UUID tourId, int count) {
        return findTourOrThrow(tourId).getAvailableSeats() >= count;
    }

    private Tour findTourOrThrow(UUID tourId) {
        return tourRepository.findById(tourId)
              .orElseThrow(() -> new EntityNotFoundException("Tour", tourId));
    }

    private void notifyChanged(Tour tour, int remaining) {
        listeners.forEach(l -> {
            try { l.onQuotaChanged(tour, remaining); }
            catch (Exception e) { log.warn("QuotaListener помилка: {}", e.getMessage()); }
        });
    }

    private void notifyExceeded(Tour tour) {
        listeners.forEach(l -> {
            try { l.onQuotaExceeded(tour); }
            catch (Exception e) { log.warn("QuotaListener помилка: {}", e.getMessage()); }
        });
    }

    public interface QuotaListener {
        void onQuotaChanged(Tour tour, int remainingSeats);
        void onQuotaExceeded(Tour tour);
    }
}
