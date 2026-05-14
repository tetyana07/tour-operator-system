package com.touroperator.repository;

import com.touroperator.domain.Transfer;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransferRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Transfer> MAPPER = (rs, rowNum) -> {
        Transfer t = new Transfer();
        t.setId(UUID.fromString(rs.getString("id")));
        t.setType(rs.getString("type"));
        t.setPrice(rs.getBigDecimal("price"));
        return t;
    };

    public TransferRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Transfer> findById(UUID id) {
        try {
            Transfer t = jdbc.queryForObject(
                  "SELECT * FROM transfers WHERE id = ?", MAPPER, id);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Transfer> findAll() {
        return jdbc.query("SELECT * FROM transfers ORDER BY price", MAPPER);
    }

    public void saveBookingTransfer(UUID bookingId, UUID transferId) {
        jdbc.update("""
                INSERT INTO booking_transfers (booking_id, transfer_id)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """,
              bookingId, transferId);
    }
}