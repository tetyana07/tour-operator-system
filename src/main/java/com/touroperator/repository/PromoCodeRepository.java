package com.touroperator.repository;

import com.touroperator.domain.PromoCode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public class PromoCodeRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<PromoCode> MAPPER = (rs, rowNum) -> {
        PromoCode p = new PromoCode();
        p.setId(UUID.fromString(rs.getString("id")));
        p.setCode(rs.getString("code"));
        p.setDiscountPercent(rs.getInt("discount_percent"));
        java.sql.Date until = rs.getDate("valid_until");
        if (until != null) p.setValidUntil(until.toLocalDate());
        return p;
    };

    public PromoCodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public Optional<PromoCode> findByCode(String code) {
        try {
            PromoCode p = jdbc.queryForObject(
                  "SELECT * FROM promo_codes WHERE UPPER(code) = UPPER(?)",
                  MAPPER, code);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<PromoCode> findById(UUID id) {
        try {
            PromoCode p = jdbc.queryForObject(
                  "SELECT * FROM promo_codes WHERE id = ?",
                  MAPPER, id);
            return Optional.ofNullable(p);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(PromoCode promoCode) {
        if (promoCode.getId() == null) promoCode.setId(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO promo_codes (id, code, discount_percent, valid_until)
                VALUES (?, ?, ?, ?)
                """,
              promoCode.getId(),
              promoCode.getCode(),
              promoCode.getDiscountPercent(),
              promoCode.getValidUntil() != null ? java.sql.Date.valueOf(promoCode.getValidUntil()) : null
        );
    }

    public List<PromoCode> findAll() {
        return jdbc.query("SELECT * FROM promo_codes ORDER BY code", MAPPER);
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM promo_codes WHERE id = ?", id);
    }
}