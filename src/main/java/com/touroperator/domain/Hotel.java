package com.touroperator.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Hotel {
    private UUID id;
    private String name;
    private int stars;
    private BigDecimal pricePerNight;

    public Hotel() {}

    public Hotel(String name, int stars, BigDecimal pricePerNight) {
        this.name = name;
        this.stars = stars;
        this.pricePerNight = pricePerNight;
    }

    public Hotel(UUID id, String name, int stars, BigDecimal pricePerNight) {
        this.id = id;
        this.name = name;
        this.stars = stars;
        this.pricePerNight = pricePerNight;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    @Override
    public String toString() {
        return String.format("Hotel [id=%s, name='%s', stars=%d, pricePerNight=%s]", id, name, stars, pricePerNight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hotel hotel = (Hotel) o;
        return id.equals(hotel.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}