package com.touroperator.repository;

import com.touroperator.domain.Hotel;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class HotelRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Hotel> MAPPER = (rs, rowNum) -> {
        Hotel h = new Hotel();
        h.setId(UUID.fromString(rs.getString("id")));
        h.setName(rs.getString("name"));
        h.setStars(rs.getInt("stars"));
        h.setPricePerNight(rs.getBigDecimal("price_per_night"));
        return h;
    };

    public HotelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Hotel> findById(UUID id) {
        try {
            Hotel h = jdbc.queryForObject(
                  "SELECT * FROM hotels WHERE id = ?", MAPPER, id);
            return Optional.ofNullable(h);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Hotel> findAll() {
        return jdbc.query("SELECT * FROM hotels ORDER BY stars DESC, name", MAPPER);
    }

    public void save(Hotel hotel) {
        if (hotel.getId() == null) hotel.setId(java.util.UUID.randomUUID());
        jdbc.update(
              "INSERT INTO hotels (id, name, stars, price_per_night) VALUES (?, ?, ?, ?)",
              hotel.getId(),
              hotel.getName(),
              hotel.getStars(),
              hotel.getPricePerNight()
        );
    }
}