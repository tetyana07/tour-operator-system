package com.touroperator.mapper;

import com.touroperator.domain.Booking;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class BookingRowMapper implements RowMapper<Booking> {
    @Override
    public Booking mapRow(ResultSet rs, int rowNum) throws SQLException {
        Booking b = new Booking();
        b.setId(UUID.fromString(rs.getString("id")));
        b.setClientId(UUID.fromString(rs.getString("client_id")));
        b.setTourId(UUID.fromString(rs.getString("tour_id")));
        String promoId = rs.getString("promo_code_id");
        b.setPromoCodeId(promoId != null ? UUID.fromString(promoId) : null);
        b.setTouristCount(rs.getInt("tourist_count"));
        java.sql.Date bd = rs.getDate("booking_date");
        b.setBookingDate(bd != null ? bd.toLocalDate() : java.time.LocalDate.now());
        b.setStatus(rs.getString("status"));
        b.setTotalPrice(rs.getBigDecimal("total_price"));
        Timestamp confirmedAt = rs.getTimestamp("confirmed_at");
        if (confirmedAt != null) b.setConfirmedAt(confirmedAt.toLocalDateTime());
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        if (cancelledAt != null) b.setCancelledAt(cancelledAt.toLocalDateTime());
        b.setCancelReason(rs.getString("cancel_reason"));
        return b;
    }
}