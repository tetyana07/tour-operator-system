package com.touroperator.repository;

import com.touroperator.domain.Client;
import com.touroperator.identity.IdentityMap;
import com.touroperator.mapper.ClientRowMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public class ClientRepository {

    private final JdbcTemplate jdbc;
    private final IdentityMap identityMap;
    private final ClientRowMapper mapper = new ClientRowMapper();

    public ClientRepository(JdbcTemplate jdbc, IdentityMap identityMap) {
        this.jdbc = jdbc;
        this.identityMap = identityMap;
    }

    public Optional<Client> findById(UUID id) {
        Optional<Client> cached = identityMap.getClient(id);
        if (cached.isPresent()) return cached;
        try {
            Client c = jdbc.queryForObject(
                  "SELECT * FROM clients WHERE id = ?",
                  mapper, id);
            if (c != null) identityMap.putClient(c);
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Client> findByEmail(String email) {
        try {
            Client c = jdbc.queryForObject(
                  "SELECT * FROM clients WHERE email = ?",
                  mapper, email);
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Client> findAll() {
        return jdbc.query("SELECT * FROM clients ORDER BY name", mapper);
    }

    public void saveClient(Client client) {
        if (client.getId() == null) client.setId(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO clients (id, name, email, phone, birth_date, password_hash)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (email) DO UPDATE
                  SET name=EXCLUDED.name,
                      phone=EXCLUDED.phone,
                      birth_date=EXCLUDED.birth_date,
                      password_hash=COALESCE(EXCLUDED.password_hash, clients.password_hash)
                """,
              client.getId(),
              client.getName(),
              client.getEmail(),
              client.getPhone(),
              client.getBirthDate() != null ? java.sql.Date.valueOf(client.getBirthDate()) : null,
              client.getPasswordHash()
        );
        identityMap.putClient(client);
    }

    public void update(Client client) {
        jdbc.update("""
                UPDATE clients SET name=?, email=?, phone=?, birth_date=? WHERE id=?
                """,
              client.getName(), client.getEmail(),
              client.getPhone(), (client.getBirthDate() != null ? java.sql.Date.valueOf(client.getBirthDate()) : null),
              client.getId());
        identityMap.putClient(client);
    }

    public void updatePasswordHash(String email, String passwordHash) {
        jdbc.update("UPDATE clients SET password_hash=? WHERE email=?", passwordHash, email);
        // email-based update — інвалідуємо весь клієнт якщо він є в кеші
        findByEmail(email).ifPresent(c -> identityMap.putClient(c));
    }

    public void delete(UUID id) {
        jdbc.update("DELETE FROM clients WHERE id = ?", id);
        identityMap.invalidateClient(id);
    }
}