package com.touroperator.repository;

import com.touroperator.domain.Tour;
import com.touroperator.domain.TourStatus;
import com.touroperator.identity.IdentityMap;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public class TourRepository {

    private final JdbcTemplate jdbc;
    private final IdentityMap identityMap;

    public TourRepository(JdbcTemplate jdbc, IdentityMap identityMap) {
        this.jdbc = jdbc;
        this.identityMap = identityMap;
    }

    private static final RowMapper<Tour> TOUR_MAPPER = (rs, rowNum) -> {
        Tour t = new Tour();
        t.setId(UUID.fromString(rs.getString("id")));
        t.setName(rs.getString("name"));
        t.setCountry(rs.getString("country"));
        t.setCity(rs.getString("city"));
        t.setStartDate(rs.getDate("start_date").toLocalDate());
        t.setEndDate(rs.getDate("end_date").toLocalDate());
        t.setBasePrice(rs.getBigDecimal("base_price"));
        t.setQuota(rs.getInt("quota"));
        t.setBookedSeats(rs.getInt("booked_seats"));
        String hotelId = rs.getString("hotel_id");
        if (hotelId != null) t.setHotelId(UUID.fromString(hotelId));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try { t.setStatus(TourStatus.valueOf(statusStr)); }
            catch (IllegalArgumentException ignored) { t.setStatus(TourStatus.ACTIVE); }
        }
        t.setImagePath(rs.getString("image_path"));
        // Що включено
        t.setMealType(rs.getString("meal_type"));
        t.setHasFlight(rs.getBoolean("has_flight"));
        t.setHasTransfer(rs.getBoolean("has_transfer"));
        t.setHasInsurance(rs.getBoolean("has_insurance"));
        t.setHasGuide(rs.getBoolean("has_guide"));
        t.setDeparture(rs.getString("departure"));
        return t;
    };


    public Optional<Tour> findById(UUID id) {
        Optional<Tour> cached = identityMap.getTour(id);
        if (cached.isPresent()) return cached;
        try {
            Tour t = jdbc.queryForObject(
                  "SELECT * FROM tours WHERE id = ?",
                  TOUR_MAPPER, id);
            if (t != null) identityMap.putTour(t);
            return Optional.ofNullable(t);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public List<Tour> findAll() {
        return jdbc.query("SELECT * FROM tours ORDER BY start_date", TOUR_MAPPER);
    }

    /** Пагінація турів */
    public List<Tour> findAll(int page, int pageSize) {
        return jdbc.query(
              "SELECT * FROM tours ORDER BY start_date LIMIT ? OFFSET ?",
              TOUR_MAPPER, pageSize, (long) page * pageSize);
    }

    public int countAll() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM tours", Integer.class);
        return c != null ? c : 0;
    }

    /**
     * Повнотекстовий пошук по назві, країні, місту.
     */
    public List<Tour> search(String query) {
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return jdbc.query(
              """
              SELECT * FROM tours
              WHERE LOWER(name) LIKE ?
                 OR LOWER(country) LIKE ?
                 OR LOWER(city) LIKE ?
              ORDER BY start_date
              LIMIT 100
              """,
              TOUR_MAPPER, pattern, pattern, pattern);
    }


    public List<Tour> findByStatus(com.touroperator.domain.TourStatus status) {
        return jdbc.query(
              "SELECT * FROM tours WHERE status = ? ORDER BY start_date",
              TOUR_MAPPER, status.name());
    }


    public List<Tour> findActive() {
        return jdbc.query(
              "SELECT * FROM tours WHERE status = 'ACTIVE' ORDER BY start_date",
              TOUR_MAPPER);
    }


    public void save(Tour tour) {
        if (tour.getId() == null) tour.setId(UUID.randomUUID());
        jdbc.update(
              "INSERT INTO tours (id, name, country, city, start_date, end_date," +
                    " base_price, quota, booked_seats, hotel_id, image_path, status," +
                    " meal_type, has_flight, has_transfer, has_insurance, has_guide, departure)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?, ?, ?)",
              tour.getId(),
              tour.getName(),
              tour.getCountry(),
              tour.getCity(),
              java.sql.Date.valueOf(tour.getStartDate()),
              java.sql.Date.valueOf(tour.getEndDate()),
              tour.getBasePrice(),
              tour.getQuota(),
              tour.getBookedSeats(),
              tour.getHotelId() != null ? tour.getHotelId() : null,
              tour.getImagePath(),
              tour.getMealType(),
              tour.isHasFlight(),
              tour.isHasTransfer(),
              tour.isHasInsurance(),
              tour.isHasGuide(),
              tour.getDeparture()
        );
        identityMap.putTour(tour);
    }


    public void update(Tour tour) {
        jdbc.update(
              "UPDATE tours SET name=?, country=?, city=?, start_date=?, end_date=?," +
                    " base_price=?, quota=?, booked_seats=?, hotel_id=?, image_path=?," +
                    " meal_type=?, has_flight=?, has_transfer=?, has_insurance=?, has_guide=?, departure=? WHERE id=?",
              tour.getName(),
              tour.getCountry(),
              tour.getCity(),
              java.sql.Date.valueOf(tour.getStartDate()),
              java.sql.Date.valueOf(tour.getEndDate()),
              tour.getBasePrice(),
              tour.getQuota(),
              tour.getBookedSeats(),
              tour.getHotelId() != null ? tour.getHotelId() : null,
              tour.getImagePath(),
              tour.getMealType(),
              tour.isHasFlight(),
              tour.isHasTransfer(),
              tour.isHasInsurance(),
              tour.isHasGuide(),
              tour.getDeparture(),
              tour.getId()
        );
        identityMap.invalidateTour(tour.getId());
    }


    public void incrementBookedSeats(UUID tourId, int count) {
        jdbc.update("""
                UPDATE tours
                SET booked_seats = booked_seats + ?,
                    status = CASE
                        WHEN booked_seats + ? >= quota THEN 'FULL'
                        ELSE 'ACTIVE'
                    END
                WHERE id = ?
                """, count, count, tourId);
        identityMap.invalidateTour(tourId);
    }


    public void decrementBookedSeats(UUID tourId, int count) {
        jdbc.update("""
                UPDATE tours
                SET booked_seats = GREATEST(0, booked_seats - ?),
                    status = CASE
                        WHEN status = 'FULL' THEN 'ACTIVE'
                        ELSE status
                    END
                WHERE id = ?
                """, count, tourId);
        identityMap.invalidateTour(tourId);
    }


    public void setStatus(UUID tourId, TourStatus status) {
        jdbc.update("UPDATE tours SET status = ? WHERE id = ?",
              status.name(), tourId);
        identityMap.invalidateTour(tourId);
    }
}