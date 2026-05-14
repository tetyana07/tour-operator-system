package com.touroperator.mapper;

import com.touroperator.domain.BookingTransfer;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingTransferRowMapper implements RowMapper<BookingTransfer> {
    @Override
    public BookingTransfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        BookingTransfer bookingTransfer = new BookingTransfer();
        bookingTransfer.setBookingId(UUID.fromString(rs.getString("booking_id")));
        bookingTransfer.setTransferId(UUID.fromString(rs.getString("transfer_id")));
        return bookingTransfer;
    }
}