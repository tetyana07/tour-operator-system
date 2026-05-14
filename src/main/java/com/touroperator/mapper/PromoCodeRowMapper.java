package com.touroperator.mapper;

import com.touroperator.domain.PromoCode;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PromoCodeRowMapper implements RowMapper<PromoCode> {
    @Override
    public PromoCode mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PromoCode(
              UUID.fromString(rs.getString("id")),
              rs.getString("code"),
              rs.getInt("discount_percent"),
              rs.getDate("valid_until").toLocalDate()
        );
    }
}
