package com.touroperator.mapper;

import com.touroperator.domain.Excursion;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ExcursionRowMapper implements RowMapper<Excursion> {
    @Override
    public Excursion mapRow(ResultSet rs, int rowNum) throws SQLException {
        Excursion excursion = new Excursion();
        excursion.setId(UUID.fromString(rs.getString("id")));
        excursion.setName(rs.getString("name"));
        excursion.setPrice(rs.getBigDecimal("price"));
        excursion.setExcursionDate(rs.getDate("excursion_date").toLocalDate());
        return excursion;
    }
}