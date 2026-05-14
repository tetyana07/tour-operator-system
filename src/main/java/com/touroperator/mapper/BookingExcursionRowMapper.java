package com.touroperator.mapper;

import com.touroperator.domain.BookingExcursion;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingExcursionRowMapper implements RowMapper<BookingExcursion> {
    @Override
    public BookingExcursion mapRow(ResultSet rs, int rowNum) throws SQLException {
        BookingExcursion bookingExcursion = new BookingExcursion();
        bookingExcursion.setBookingId(UUID.fromString(rs.getString("booking_id")));
        bookingExcursion.setExcursionId(UUID.fromString(rs.getString("excursion_id")));
        return bookingExcursion;
    }
}