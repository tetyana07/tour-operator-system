package com.touroperator.uow;

import com.touroperator.domain.Booking;
import com.touroperator.domain.Tour;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.TourRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit of Work — збирає зміни і зберігає їх одним commit().
 */
@Component
public class UnitOfWork {

    private final Set<Booking> newBookings     = new HashSet<>();
    private final Set<Booking> dirtyBookings   = new HashSet<>();
    private final Set<Booking> removedBookings = new HashSet<>();
    private final Set<Tour>    newTours        = new HashSet<>();
    private final Set<Tour>    dirtyTours      = new HashSet<>();
    private final Set<Tour>    removedTours    = new HashSet<>();

    private final BookingRepository bookingRepository;
    private final TourRepository    tourRepository;

    public UnitOfWork(BookingRepository bookingRepository, TourRepository tourRepository) {
        this.bookingRepository = bookingRepository;
        this.tourRepository    = tourRepository;
    }

    public void registerNewBooking(Booking b)     { newBookings.add(b); }
    public void registerDirtyBooking(Booking b)   { dirtyBookings.add(b); }
    public void registerRemovedBooking(Booking b) { removedBookings.add(b); }

    public void registerNewTour(Tour t)     { newTours.add(t); }
    public void registerDirtyTour(Tour t)   { dirtyTours.add(t); }
    public void registerRemovedTour(Tour t) { removedTours.add(t); }

    /** Зберегти всі зміни в БД. */
    public void commit() {
        newBookings  .forEach(bookingRepository::save);
        dirtyBookings.forEach(bookingRepository::save);
        removedBookings.forEach(b ->
              bookingRepository.cancel(b.getId(), "Видалено через UnitOfWork"));

        newTours  .forEach(tourRepository::save);
        dirtyTours.forEach(tourRepository::update);
        removedTours.forEach(t ->
              tourRepository.setStatus(t.getId(),
                    com.touroperator.domain.TourStatus.CANCELLED));
        clear();
    }

    private void clear() {
        newBookings.clear();     dirtyBookings.clear();   removedBookings.clear();
        newTours.clear();        dirtyTours.clear();      removedTours.clear();
    }
}
