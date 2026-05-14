package com.touroperator.service;

import com.touroperator.domain.Tour;
import com.touroperator.domain.TourStatus;
import com.touroperator.exception.EntityNotFoundException;
import com.touroperator.repository.TourRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TourService {

    private final TourRepository tourRepo;

    public TourService(TourRepository tourRepo) {
        this.tourRepo = tourRepo;
    }


    public Tour createTour(Tour tour) {
        if (tour.getStartDate() == null || tour.getEndDate() == null)
            throw new IllegalArgumentException("Дати початку і завершення є обов'язковими");
        if (!tour.getEndDate().isAfter(tour.getStartDate()))
            throw new IllegalArgumentException(
                  "Дата завершення має бути пізніше дати початку. " +
                        "Початок: " + tour.getStartDate() + ", Кінець: " + tour.getEndDate());
        if (tour.getStartDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Дата початку туру не може бути в минулому");
        if (tour.getQuota() <= 0)
            throw new IllegalArgumentException("Квота місць має бути більше 0, отримано: " + tour.getQuota());
        if (tour.getBasePrice() == null || tour.getBasePrice().signum() <= 0)
            throw new IllegalArgumentException("Базова ціна має бути більше 0");

        tour.setId(UUID.randomUUID());
        tour.setBookedSeats(0);
        tourRepo.save(tour);
        return tour;
    }

    public Tour getTourById(UUID id) {
        return tourRepo.findById(id)
              .orElseThrow(() -> new EntityNotFoundException("Тур", id));
    }

    public List<Tour> findAll() {
        return tourRepo.findAll();
    }

    public List<Tour> findByStatus(com.touroperator.domain.TourStatus status) {
        return tourRepo.findByStatus(status);
    }

    public List<Tour> getAllTours() {
        return tourRepo.findAll();
    }

    public List<Tour> getActiveTours() {
        return tourRepo.findActive();
    }

    public Tour updateTour(Tour tour) {
        getTourById(tour.getId());
        tourRepo.update(tour);
        return tour;
    }

    public void cancelTour(UUID tourId) {
        getTourById(tourId);
        tourRepo.setStatus(tourId, TourStatus.CANCELLED);
    }

    public void archiveTour(UUID tourId) {
        getTourById(tourId);
        tourRepo.setStatus(tourId, TourStatus.ARCHIVED);
    }
}