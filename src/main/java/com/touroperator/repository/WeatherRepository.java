package com.touroperator.repository;

import com.touroperator.domain.WeatherForecast;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


@Repository
public class WeatherRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<WeatherForecast> MAPPER = (rs, rowNum) -> {
        WeatherForecast wf = new WeatherForecast();
        wf.setId(UUID.fromString(rs.getString("id")));
        wf.setCity(rs.getString("city"));
        wf.setForecastDate(rs.getDate("forecast_date").toLocalDate());
        wf.setTemperature(rs.getBigDecimal("temperature"));
        wf.setDescription(rs.getString("description"));
        // Зчитуємо розширені поля погоди
        int humidity = rs.getInt("humidity");
        if (!rs.wasNull()) wf.setHumidity(humidity);
        int windSpeed = rs.getInt("wind_speed");
        if (!rs.wasNull()) wf.setWindSpeed(windSpeed);
        wf.setFeelsLike(rs.getBigDecimal("feels_like"));
        return wf;
    };

    public WeatherRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<WeatherForecast> findFreshCache(String city) {
        try {
            WeatherForecast wf = jdbc.queryForObject("""
                    SELECT * FROM weather_forecasts
                    WHERE LOWER(city) = LOWER(?)
                      AND forecast_date = ?
                      AND fetched_at > NOW() - INTERVAL '3 hours'
                    ORDER BY fetched_at DESC
                    LIMIT 1
                    """,
                  MAPPER, city, LocalDate.now());
            return Optional.ofNullable(wf);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<WeatherForecast> findLatestForCity(String city) {
        try {
            WeatherForecast wf = jdbc.queryForObject("""
                    SELECT * FROM weather_forecasts
                    WHERE LOWER(city) = LOWER(?)
                      AND fetched_at > NOW() - INTERVAL '24 hours'
                    ORDER BY fetched_at DESC
                    LIMIT 1
                    """,
                  MAPPER, city);
            return Optional.ofNullable(wf);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Видаляє записи старіші за 24 години */
    public void deleteStale() {
        jdbc.update("DELETE FROM weather_forecasts WHERE fetched_at < NOW() - INTERVAL '24 hours'");
    }


    public void save(WeatherForecast wf) {
        jdbc.update("""
                INSERT INTO weather_forecasts (city, forecast_date, temperature, description, humidity, wind_speed, feels_like, fetched_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (city, forecast_date) DO UPDATE
                SET temperature = EXCLUDED.temperature,
                    description = EXCLUDED.description,
                    humidity    = EXCLUDED.humidity,
                    wind_speed  = EXCLUDED.wind_speed,
                    feels_like  = EXCLUDED.feels_like,
                    fetched_at  = NOW()
                """,
              wf.getCity(),
              wf.getForecastDate(),
              wf.getTemperature(),
              wf.getDescription(),
              wf.getHumidity(),
              wf.getWindSpeed(),
              wf.getFeelsLike()
        );
    }
}