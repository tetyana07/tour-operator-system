package com.touroperator.mapper;

import com.touroperator.domain.Client;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ClientRowMapper implements RowMapper<Client> {
    @Override
    public Client mapRow(ResultSet rs, int rowNum) throws SQLException {
        Client client = new Client();
        client.setId(UUID.fromString(rs.getString("id")));
        client.setName(rs.getString("name"));
        client.setEmail(rs.getString("email"));
        client.setPhone(rs.getString("phone"));
        try { client.setPasswordHash(rs.getString("password_hash")); }
        catch (Exception ignored) { client.setPasswordHash(null); }
        try { client.setVerifyToken(rs.getString("verify_token")); }
        catch (Exception ignored) { client.setVerifyToken(null); }
        try { client.setEmailVerified(rs.getBoolean("email_verified")); }
        catch (Exception ignored) { client.setEmailVerified(false); }
        java.sql.Date bd = rs.getDate("birth_date");
        if (bd != null) client.setBirthDate(bd.toLocalDate());
        return client;
    }
}