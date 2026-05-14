package com.touroperator.mapper;

import com.touroperator.domain.Insurance;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class InsuranceRowMapper implements RowMapper<Insurance> {
    @Override
    public Insurance mapRow(ResultSet rs, int rowNum) throws SQLException {
        Insurance insurance = new Insurance();
        insurance.setId(UUID.fromString(rs.getString("id")));
        insurance.setName(rs.getString("name"));
        insurance.setPrice(rs.getBigDecimal("price"));
        return insurance;
    }
}