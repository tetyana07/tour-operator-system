package com.touroperator.repository;

import com.touroperator.domain.Payment;
import com.touroperator.domain.PaymentStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Payment> MAPPER = (rs, rowNum) -> {
        Payment p = new Payment();
        p.setId(UUID.fromString(rs.getString("id")));
        p.setBookingId(UUID.fromString(rs.getString("booking_id")));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentDate(rs.getDate("payment_date").toLocalDate());
        p.setStatus(rs.getString("status"));
        return p;
    };

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Payment> findByBookingId(UUID bookingId) {
        try {
            Payment p = jdbc.queryForObject(
                  "SELECT * FROM payments WHERE booking_id = ?",
                  MAPPER, bookingId);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Payment> findAll() {
        return jdbc.query("SELECT * FROM payments ORDER BY payment_date DESC", MAPPER);
    }


    public void save(Payment payment) {
        if (payment.getId() == null) payment.setId(UUID.randomUUID());
        if (payment.getPaymentDate() == null) payment.setPaymentDate(LocalDate.now());
        jdbc.update("""
                INSERT INTO payments (id, booking_id, amount, payment_date, status)
                VALUES (?, ?, ?, ?, ?)
                """,
              payment.getId(),
              payment.getBookingId(),
              payment.getAmount(),
              payment.getPaymentDate(),
              payment.getStatus()
        );
    }


    public void confirmByBookingId(UUID bookingId) {
        jdbc.update("UPDATE payments SET status = 'SUCCESS' WHERE booking_id = ?", bookingId);
    }


    public void updateStatus(UUID paymentId, PaymentStatus status) {
        jdbc.update("UPDATE payments SET status = ? WHERE id = ?",
              status.name(), paymentId);
    }
}