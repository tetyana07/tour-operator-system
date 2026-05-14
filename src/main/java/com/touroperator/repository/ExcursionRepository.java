package com.touroperator.repository;

import com.touroperator.domain.Excursion;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторій для екскурсій.
 */
@Repository
public class ExcursionRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Excursion> MAPPER = (rs, rowNum) -> {
        Excursion e = new Excursion();
        e.setId(UUID.fromString(rs.getString("id")));
        e.setName(rs.getString("name"));
        e.setPrice(rs.getBigDecimal("price"));
        java.sql.Date d = rs.getDate("excursion_date");
        if (d != null) e.setExcursionDate(d.toLocalDate());
        return e;
    };

    public ExcursionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Excursion> findById(UUID id) {
        try {
            Excursion e = jdbc.queryForObject(
                  "SELECT * FROM excursions WHERE id = ?", MAPPER, id);
            return Optional.ofNullable(e);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** Екскурсії доступні для конкретного туру */
    public List<Excursion> findByTourId(UUID tourId) {
        return jdbc.query("""
                SELECT e.* FROM excursions e
                JOIN tour_excursions te ON te.excursion_id = e.id
                WHERE te.tour_id = ?
                """, MAPPER, tourId);
    }

    public List<Excursion> findAll() {
        return jdbc.query("SELECT * FROM excursions ORDER BY name", MAPPER);
    }

    public void save(Excursion excursion) {
        if (excursion.getId() == null) excursion.setId(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO excursions (id, name, price, excursion_date)
                VALUES (?, ?, ?, ?)
                """,
              excursion.getId(),
              excursion.getName(),
              excursion.getPrice(),
              excursion.getExcursionDate() != null ? java.sql.Date.valueOf(excursion.getExcursionDate()) : null
        );
    }

    /** Зв'язати екскурсію з туром */
    public void linkToTour(UUID tourId, UUID excursionId) {
        jdbc.update("""
                INSERT INTO tour_excursions (tour_id, excursion_id)
                VALUES (?, ?) ON CONFLICT DO NOTHING
                """,
              tourId, excursionId);
    }

    /** Зберегти вибрані екскурсії до бронювання */
    public void saveBookingExcursions(UUID bookingId, List<UUID> excursionIds) {
        for (UUID excId : excursionIds) {
            jdbc.update("""
                    INSERT INTO booking_excursions (booking_id, excursion_id)
                    VALUES (?, ?) ON CONFLICT DO NOTHING
                    """,
                  bookingId, excId);
        }
    }

    /** Отримати екскурсії бронювання */
    public List<Excursion> findByBookingId(UUID bookingId) {
        return jdbc.query("""
                SELECT e.* FROM excursions e
                JOIN booking_excursions be ON be.excursion_id = e.id
                WHERE be.booking_id = ?
                """, MAPPER, bookingId);
    }
}