package com.touroperator.repository;

import com.touroperator.domain.Insurance;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InsuranceRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Insurance> MAPPER = (rs, rowNum) -> {
        Insurance ins = new Insurance();
        ins.setId(UUID.fromString(rs.getString("id")));
        ins.setName(rs.getString("name"));
        ins.setPrice(rs.getBigDecimal("price"));
        return ins;
    };

    public InsuranceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Insurance> findById(UUID id) {
        try {
            Insurance ins = jdbc.queryForObject(
                  "SELECT * FROM insurances WHERE id = ?", MAPPER, id);
            return Optional.ofNullable(ins);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Insurance> findAll() {
        return jdbc.query("SELECT * FROM insurances ORDER BY price", MAPPER);
    }

    /** Зберегти обрану страховку до бронювання */
    public void saveBookingInsurance(UUID bookingId, UUID insuranceId) {
        jdbc.update("""
                INSERT INTO booking_insurances (booking_id, insurance_id)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """,
              bookingId, insuranceId);
    }
}