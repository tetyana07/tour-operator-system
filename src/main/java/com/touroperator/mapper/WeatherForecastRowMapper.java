package com.touroperator.mapper;

import com.touroperator.domain.WeatherForecast;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class WeatherForecastRowMapper
      implements RowMapper<WeatherForecast> {

    @Override
    public WeatherForecast mapRow(
          ResultSet rs,
          int rowNum
    ) throws SQLException {

        WeatherForecast weatherForecast =
              new WeatherForecast();

        weatherForecast.setId(
              UUID.fromString(rs.getString("id"))
        );

        weatherForecast.setCity(
              rs.getString("city")
        );

        weatherForecast.setForecastDate(
              rs.getDate("forecast_date")
                    .toLocalDate()
        );

        weatherForecast.setTemperature(
              rs.getBigDecimal("temperature")
        );

        weatherForecast.setDescription(
              rs.getString("description")
        );

        return weatherForecast;
    }
}