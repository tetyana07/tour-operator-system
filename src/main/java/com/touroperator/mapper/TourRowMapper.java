package com.touroperator.mapper;

import com.touroperator.domain.Tour;
import com.touroperator.domain.TourStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TourRowMapper implements RowMapper<Tour> {
    @Override
    public Tour mapRow(ResultSet rs, int rowNum) throws SQLException {
        Tour t = new Tour(
              UUID.fromString(rs.getString("id")),
              rs.getString("name"),
              rs.getString("country"),
              rs.getString("city"),
              rs.getDate("start_date").toLocalDate(),
              rs.getDate("end_date").toLocalDate(),
              rs.getBigDecimal("base_price"),
              rs.getInt("quota"),
              rs.getInt("booked_seats"),
              rs.getString("hotel_id") != null ? UUID.fromString(rs.getString("hotel_id")) : null
        );
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try { t.setStatus(TourStatus.valueOf(statusStr)); }
            catch (IllegalArgumentException ignored) { t.setStatus(TourStatus.ACTIVE); }
        }
        t.setImagePath(rs.getString("image_path"));

        t.setMealType(rs.getString("meal_type"));
        t.setHasFlight(rs.getBoolean("has_flight"));
        t.setHasTransfer(rs.getBoolean("has_transfer"));
        t.setHasInsurance(rs.getBoolean("has_insurance"));
        t.setHasGuide(rs.getBoolean("has_guide"));
        t.setDeparture(rs.getString("departure"));
        return t;
    }
}