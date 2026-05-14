package com.touroperator.repository;

import com.touroperator.domain.Booking;
import com.touroperator.domain.BookingStatus;
import com.touroperator.mapper.BookingRowMapper;
import com.touroperator.specification.Specification;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public class BookingRepository {

    private final JdbcTemplate jdbc;
    private final BookingRowMapper mapper = new BookingRowMapper();

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public Optional<Booking> findById(UUID id) {
        try {
            Booking b = jdbc.queryForObject(
                  "SELECT * FROM bookings WHERE id = ?",
                  mapper, id);
            return Optional.ofNullable(b);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public List<Booking> findByClientId(UUID clientId) {
        return jdbc.query(
              "SELECT * FROM bookings WHERE client_id = ? ORDER BY booking_date DESC",
              mapper, clientId);
    }


    public List<Booking> findByTourId(UUID tourId) {
        return jdbc.query(
              "SELECT * FROM bookings WHERE tour_id = ? ORDER BY booking_date DESC",
              mapper, tourId);
    }


    public List<Booking> findAll() {
        return jdbc.query("SELECT * FROM bookings ORDER BY booking_date DESC", mapper);
    }


    public List<Booking> findByStatus(BookingStatus status) {
        return jdbc.query(
              "SELECT * FROM bookings WHERE status = ? ORDER BY booking_date DESC",
              mapper, status.name());
    }


    public void save(Booking booking) {
        if (booking.getId() == null) booking.setId(UUID.randomUUID());
        if (booking.getBookingDate() == null) booking.setBookingDate(LocalDate.now());
        jdbc.update("""
                INSERT INTO bookings (id, client_id, tour_id, promo_code_id, tourist_count,
                                      booking_date, status, total_price, confirmed_at, cancelled_at, cancel_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
              booking.getId(),
              booking.getClientId(),
              booking.getTourId(),
              booking.getPromoCodeId() != null ? booking.getPromoCodeId() : null,
              booking.getTouristCount(),
              java.sql.Date.valueOf(booking.getBookingDate()),
              booking.getStatus(),
              booking.getTotalPrice(),
              booking.getConfirmedAt(),
              booking.getCancelledAt(),
              booking.getCancelReason()
        );
    }


    public void updateStatus(UUID bookingId, BookingStatus status) {
        jdbc.update("UPDATE bookings SET status = ? WHERE id = ?",
              status.name(), bookingId);
    }


    public void confirm(UUID bookingId) {
        jdbc.update("UPDATE bookings SET status = 'CONFIRMED', confirmed_at = ? WHERE id = ?",
              LocalDateTime.now(), bookingId);
    }


    public void cancel(UUID bookingId, String reason) {
        jdbc.update("""
                UPDATE bookings SET status = 'CANCELLED', cancelled_at = ?, cancel_reason = ?
                WHERE id = ?
                """,
              LocalDateTime.now(), reason, bookingId);
    }


    public void markPaid(UUID bookingId) {
        jdbc.update("UPDATE bookings SET status = 'PAID' WHERE id = ?",
              bookingId);
    }


    public void markCompleted(UUID bookingId) {
        jdbc.update("UPDATE bookings SET status = 'COMPLETED' WHERE id = ?",
              bookingId);
    }


    public boolean existsActiveByClientAndTour(UUID clientId, UUID tourId) {
        Integer count = jdbc.queryForObject(
              """
              SELECT COUNT(*) FROM bookings
              WHERE client_id = ? AND tour_id = ?
                AND status IN ('CREATED', 'CONFIRMED', 'PAID')
              """,
              Integer.class, clientId, tourId);
        return count != null && count > 0;
    }

    /**
     * Пошук бронювань за довільним Specification.
     * Приклад: findBySpec(new BookingByClientEmailSpec("user@example.com"))
     */
    public List<Booking> findBySpec(Specification spec) {
        String sql = "SELECT * FROM bookings WHERE " + spec.toSql() + " ORDER BY booking_date DESC";
        return jdbc.query(sql, mapper, spec.getParams());
    }

}