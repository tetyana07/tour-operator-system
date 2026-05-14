package com.touroperator.mapper;

import com.touroperator.domain.Transfer;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TransferRowMapper implements RowMapper<Transfer> {
    @Override
    public Transfer mapRow(ResultSet rs, int rowNum) throws SQLException {
        Transfer transfer = new Transfer();
        transfer.setId(UUID.fromString(rs.getString("id")));
        transfer.setType(rs.getString("type"));
        transfer.setPrice(rs.getBigDecimal("price"));
        return transfer;
    }
}