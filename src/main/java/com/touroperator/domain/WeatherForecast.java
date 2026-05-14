package com.touroperator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class WeatherForecast {
    private UUID id;
    private String city;
    private LocalDate forecastDate;
    private BigDecimal temperature;
    private String description;
    private Integer humidity;
    private Integer windSpeed;
    private BigDecimal feelsLike;

    public WeatherForecast() {}

    public static WeatherForecast unknown(String city) {
        WeatherForecast wf = new WeatherForecast();
        wf.setCity(city);
        wf.setForecastDate(LocalDate.now());
        wf.setTemperature(BigDecimal.ZERO);
        wf.setDescription("Немає даних (офлайн)");
        return wf;
    }

    public String getFormattedTemp() {
        if (temperature == null) return "N/A";
        return (temperature.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
              temperature.intValue() + "\u00B0C";   // °C через unicode — без проблем з кодуванням
    }

    public String getFormattedHumidity() {
        return humidity != null ? humidity + "%" : "\u2014";
    }

    public String getFormattedWind() {
        return windSpeed != null ? windSpeed + " км/год" : "\u2014";
    }

    public String getFormattedFeelsLike() {
        if (feelsLike == null) return "\u2014";
        return (feelsLike.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") +
              feelsLike.intValue() + "\u00B0C";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getForecastDate() { return forecastDate; }
    public void setForecastDate(LocalDate forecastDate) { this.forecastDate = forecastDate; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getHumidity() { return humidity; }
    public void setHumidity(Integer humidity) { this.humidity = humidity; }
    public Integer getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Integer windSpeed) { this.windSpeed = windSpeed; }
    public BigDecimal getFeelsLike() { return feelsLike; }
    public void setFeelsLike(BigDecimal feelsLike) { this.feelsLike = feelsLike; }
}