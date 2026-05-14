package com.touroperator.mapper;

import com.touroperator.domain.Hotel;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class HotelRowMapper implements RowMapper<Hotel> {
    @Override
    public Hotel mapRow(ResultSet rs, int rowNum) throws SQLException {
        Hotel hotel = new Hotel();
        hotel.setId(UUID.fromString(rs.getString("id")));
        hotel.setName(rs.getString("name"));
        hotel.setStars(rs.getInt("stars"));
        hotel.setPricePerNight(rs.getBigDecimal("price_per_night"));
        return hotel;
    }
}