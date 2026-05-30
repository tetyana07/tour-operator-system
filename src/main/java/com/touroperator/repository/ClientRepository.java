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

    /** Пагінація клієнтів */
    public List<Client> findAll(int page, int pageSize) {
        return jdbc.query(
              "SELECT * FROM clients ORDER BY name LIMIT ? OFFSET ?",
              mapper, pageSize, (long) page * pageSize);
    }

    public int countAll() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM clients", Integer.class);
        return c != null ? c : 0;
    }

    /**
     * Повнотекстовий пошук по імені, email, телефону.
     * Використовує ILIKE — безпечно (параметризовано, без SQL injection).
     */
    public List<Client> search(String query) {
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return jdbc.query(
              """
              SELECT * FROM clients
              WHERE LOWER(name) LIKE ?
                 OR LOWER(email) LIKE ?
                 OR LOWER(phone) LIKE ?
              ORDER BY name
              LIMIT 100
              """,
              mapper, pattern, pattern, pattern);
    }

    public void saveClient(Client client) {
        if (client.getId() == null) client.setId(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO clients (id, name, email, phone, birth_date, password_hash, verify_token, email_verified)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (email) DO UPDATE
                  SET name=EXCLUDED.name,
                      phone=EXCLUDED.phone,
                      birth_date=EXCLUDED.birth_date,
                      password_hash=COALESCE(EXCLUDED.password_hash, clients.password_hash),
                      verify_token=COALESCE(EXCLUDED.verify_token, clients.verify_token),
                      email_verified=EXCLUDED.email_verified
                """,
              client.getId(),
              client.getName(),
              client.getEmail(),
              client.getPhone(),
              client.getBirthDate() != null ? java.sql.Date.valueOf(client.getBirthDate()) : null,
              client.getPasswordHash(),
              client.getVerifyToken(),
              client.isEmailVerified()
        );
        identityMap.putClient(client);
    }

    /** Підтвердити email за токеном. Повертає true, якщо токен знайдено. */
    public boolean verifyEmail(String token) {
        int updated = jdbc.update("""
                UPDATE clients SET email_verified = TRUE, verify_token = NULL
                WHERE verify_token = ? AND email_verified = FALSE
                """, token);
        return updated > 0;
    }

    /** Знайти клієнта за verify_token (щоб повторно надіслати лист тощо). */
    public Optional<Client> findByVerifyToken(String token) {
        try {
            Client c = jdbc.queryForObject(
                  "SELECT * FROM clients WHERE verify_token = ?",
                  mapper, token);
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
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